package br.com.datum.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** "IND." vira Indústria. Aqui serve para avisar o usuário; quem aplica de verdade é o Python. */
public final class Padronizacao {
    private Padronizacao() {}

    // Mesmo dicionário do Python (limpeza.py). Se mudar aqui, mude lá também.
    private static final Map<String, String> SEGMENTOS = Map.ofEntries(
        Map.entry("ind", "Indústria"), Map.entry("industria", "Indústria"), Map.entry("industrial", "Indústria"),
        Map.entry("com", "Comércio"), Map.entry("comercio", "Comércio"),
        Map.entry("serv", "Serviços"), Map.entry("servico", "Serviços"), Map.entry("servicos", "Serviços"),
        Map.entry("saude", "Saúde"),
        Map.entry("edu", "Educação"), Map.entry("educacao", "Educação"),
        Map.entry("tec", "Tecnologia"), Map.entry("ti", "Tecnologia"), Map.entry("tecnologia", "Tecnologia"));

    private static final Set<String> PARTICULAS = Set.of("de", "da", "do", "das", "dos", "e");

    /** "INDÚSTRIA" -> "industria", "IND." -> "ind" */
    static String simples(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Nome padrão do segmento, ou null se não for reconhecido. */
    public static String segmento(String texto) { return SEGMENTOS.get(simples(texto)); }

    /** "ANA SOUZA" -> "Ana Souza"; "maria da silva" -> "Maria da Silva" */
    public static String tituloNome(String texto) {
        if (texto == null || texto.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String palavra : texto.trim().split("\\s+")) {
            String minuscula = palavra.toLowerCase(Locale.ROOT);
            if (sb.length() > 0) sb.append(' ');
            if (sb.length() > 0 && PARTICULAS.contains(minuscula)) sb.append(minuscula);
            else sb.append(Character.toUpperCase(minuscula.charAt(0))).append(minuscula.substring(1));
        }
        return sb.toString();
    }
}
