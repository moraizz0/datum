package br.com.datum.service;

import static br.com.datum.service.Colunas.*;

import br.com.datum.dto.ClienteDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * O validador do backend: confere de novo cada linha que o front aprovou.
 * ERRO bloqueia a linha (ela não vai ao Python); AVISO só informa o que foi ajustado.
 */
@Service
public class ClienteValidacaoService {

    public record ResultadoLinha(int numero, Map<String, String> valores,
                                 List<String> erros, List<String> avisos) {
        public boolean valida() { return erros.isEmpty(); }   // válida = nenhum erro
    }

    private static final Set<String> NIVEIS = Set.of("A", "B", "C");
    private static final Pattern RE_CODIGO = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern RE_UF = Pattern.compile("^[A-Z]{2}$");
    private static final BigDecimal LIMITE_FATURAMENTO = new BigDecimal("1000000000000"); // 1 trilhão
    private static final DateTimeFormatter BR =
        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public List<ResultadoLinha> validar(List<ClienteDTO> linhas) {
        Map<String, Integer> codigos = new HashMap<>();       // código -> 1ª linha que usou
        Map<String, String> assinaturas = new HashMap<>();    // nome+faturamento+data -> código
        List<ResultadoLinha> saida = new ArrayList<>(linhas.size());

        for (ClienteDTO linha : linhas) {
            List<String> erros = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            Map<String, String> v = new LinkedHashMap<>();

            // 1) texto limpo (espaços extras e NBSP do Excel); corta o que passa do tamanho
            for (String campo : TODAS) {
                String bruto = linha.campos().get(campo);
                String limpo = limpar(bruto);
                if (bruto != null && !limpo.equals(bruto)) avisos.add(campo + ": espaços extras removidos");
                int max = TAMANHO_MAXIMO.getOrDefault(campo, Integer.MAX_VALUE);
                if (limpo.length() > max) {
                    limpo = limpo.substring(0, max).strip();
                    avisos.add(campo + ": texto cortado em " + max + " caracteres");
                }
                v.put(campo, limpo);
            }

            // 2) obrigatórios
            for (String campo : OBRIGATORIAS) {
                if (v.get(campo).isEmpty()) erros.add(campo + ": campo obrigatório vazio");
            }

            // 3) código: formato e duplicidade dentro da planilha
            String codigo = v.get(CODIGO);
            if (!codigo.isEmpty()) {
                if (!RE_CODIGO.matcher(codigo).matches()) {
                    erros.add(CODIGO + ": use só letras, números, - e _");
                }
                Integer primeira = codigos.putIfAbsent(codigo.toUpperCase(Locale.ROOT), linha.numero());
                if (primeira != null) erros.add(CODIGO + ": código duplicado (já usado na linha " + primeira + ")");
            }

            // 3b) segmento: padroniza ("ind." -> Indústria); desconhecido só gera aviso
            String segmento = v.get(SEGMENTO);
            if (!segmento.isEmpty()) {
                String padrao = Padronizacao.segmento(segmento);
                if (padrao == null) avisos.add(SEGMENTO + ": '" + segmento + "' não é um segmento conhecido");
                else if (!padrao.equals(segmento)) { avisos.add(SEGMENTO + ": padronizado para " + padrao); v.put(SEGMENTO, padrao); }
            }

            // 3c) consultor: "ANA souza" -> "Ana Souza"
            String consultor = v.get(CONSULTOR);
            if (!consultor.isEmpty()) {
                String ajustado = Padronizacao.tituloNome(consultor);
                if (!ajustado.equals(consultor)) { avisos.add(CONSULTOR + ": nome ajustado"); v.put(CONSULTOR, ajustado); }
            }

            // 4) nível A/B/C (minúscula vira aviso)
            String nivel = v.get(NIVEL);
            if (!nivel.isEmpty()) {
                if (!NIVEIS.contains(nivel.toUpperCase(Locale.ROOT))) {
                    erros.add(NIVEL + ": '" + nivel + "' inválido (use A, B ou C)");
                } else if (!NIVEIS.contains(nivel)) {
                    avisos.add(NIVEL + ": convertido para maiúscula");
                    v.put(NIVEL, nivel.toUpperCase(Locale.ROOT));
                }
            }

            // 5) faturamento: número, não negativo, abaixo de 1 trilhão; zero gera aviso
            String fat = v.get(FATURAMENTO);
            BigDecimal valorFat = null;
            if (!fat.isEmpty()) {
                Optional<BigDecimal> numero = lerDecimal(fat);
                if (numero.isEmpty()) erros.add(FATURAMENTO + ": '" + fat + "' não é um número");
                else if (numero.get().signum() < 0) erros.add(FATURAMENTO + ": não pode ser negativo");
                else if (numero.get().compareTo(LIMITE_FATURAMENTO) >= 0) erros.add(FATURAMENTO + ": valor alto demais");
                else {
                    valorFat = numero.get().setScale(2, RoundingMode.HALF_UP);
                    v.put(FATURAMENTO, valorFat.toPlainString());
                    if (valorFat.signum() == 0) avisos.add(FATURAMENTO + ": valor zero");
                }
            }

            // 6) serviços separados por ';' — pelo menos um
            String servicos = v.get(SERVICOS);
            if (!servicos.isEmpty()) {
                List<String> itens = new ArrayList<>();
                for (String s : servicos.split(";")) { String t = s.strip(); if (!t.isEmpty()) itens.add(t); }
                if (itens.isEmpty()) erros.add(SERVICOS + ": informe pelo menos um serviço");
                else v.put(SERVICOS, String.join("; ", itens));
            }

            // 7) data dd/mm/aaaa, aaaa-mm-dd ou número do Excel; a partir de 1990; futuro gera aviso
            String data = v.get(DATA);
            LocalDate dataOk = null;
            if (!data.isEmpty()) {
                Optional<LocalDate> d = lerData(data);
                if (d.isEmpty() || d.get().getYear() < 1990) erros.add(DATA + ": '" + data + "' não é uma data válida");
                else {
                    dataOk = d.get();
                    v.put(DATA, dataOk.toString());                    // guarda como aaaa-mm-dd
                    if (dataOk.isAfter(LocalDate.now())) avisos.add(DATA + ": data no futuro");
                }
            }

            // 8) UF opcional, mas se vier precisa ter 2 letras
            String uf = v.get(UF);
            if (!uf.isEmpty()) {
                if (!RE_UF.matcher(uf.toUpperCase(Locale.ROOT)).matches()) erros.add(UF + ": '" + uf + "' inválida");
                else if (!RE_UF.matcher(uf).matches()) { avisos.add(UF + ": convertida para maiúscula"); v.put(UF, uf.toUpperCase(Locale.ROOT)); }
            }

            // 9) mesmo nome + faturamento + data com outro código: possível duplicidade
            if (!v.get(NOME).isEmpty() && valorFat != null && dataOk != null && !codigo.isEmpty()) {
                String chave = Padronizacao.simples(v.get(NOME)) + "|" + valorFat.toPlainString() + "|" + dataOk;
                String outro = assinaturas.putIfAbsent(chave, codigo);
                if (outro != null && !outro.equalsIgnoreCase(codigo)) {
                    avisos.add("possível duplicidade com o cliente " + outro);
                }
            }

            saida.add(new ResultadoLinha(linha.numero(), v, erros, avisos));
        }
        return saida;
    }

    private static String limpar(String bruto) {
        if (bruto == null) return "";
        return bruto.replace('\u00A0', ' ').strip().replaceAll("\\s{2,}", " ");
    }

    /** Aceita 1850000 · 1.850.000,50 · 1850000.5 · R$ 1.850,00 · -10 */
    static Optional<BigDecimal> lerDecimal(String texto) {
        if (texto == null) return Optional.empty();
        String t = texto.replaceAll("(?i)r\\$", "").replaceAll("\\s", "");
        boolean negativo = t.startsWith("-");
        if (negativo) t = t.substring(1);
        if (t.isEmpty() || !t.matches("[0-9.,]+")) return Optional.empty();

        int ponto = t.lastIndexOf('.'), virgula = t.lastIndexOf(',');
        String n;
        if (ponto >= 0 && virgula >= 0) {                       // os dois: o último é o decimal
            char decimal = ponto > virgula ? '.' : ',';
            char milhar = decimal == '.' ? ',' : '.';
            n = t.replace(String.valueOf(milhar), "").replace(decimal, '.');
        } else if (virgula >= 0) {                              // só vírgula: 1 = decimal; várias = milhar
            n = t.indexOf(',') == virgula ? t.replace(',', '.') : t.replace(",", "");
        } else if (ponto >= 0) {                                // só ponto: 1.850.000 = milhar; 1850.5 = decimal
            n = t.matches("\\d{1,3}(\\.\\d{3})+") ? t.replace(".", "") : t;
        } else n = t;

        try {
            BigDecimal v = new BigDecimal(n);
            return Optional.of(negativo ? v.negate() : v);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static Optional<LocalDate> lerData(String texto) {
        String s = texto.strip();
        if (s.matches("\\d{5}")) {                              // número de série do Excel
            long dias = Long.parseLong(s);
            if (dias >= 1) return Optional.of(LocalDate.of(1899, 12, 30).plusDays(dias));
        }
        if (s.length() > 10 && s.charAt(10) == 'T') s = s.substring(0, 10);   // 2024-03-05T00:00:00Z
        try { return Optional.of(LocalDate.parse(s, BR)); } catch (DateTimeParseException e) { /* tenta o próximo */ }
        try { return Optional.of(LocalDate.parse(s)); } catch (DateTimeParseException e) { /* inválida */ }
        return Optional.empty();
    }
}
