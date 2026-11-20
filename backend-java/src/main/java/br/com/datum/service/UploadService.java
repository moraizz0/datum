package br.com.datum.service;

import static br.com.datum.service.Colunas.*;

import br.com.datum.config.ApiException;
import br.com.datum.config.AppProperties;
import br.com.datum.dto.ClienteDTO;
import br.com.datum.dto.UploadDtos.EnvioValidadasRequest;
import br.com.datum.dto.UploadDtos.LinhaEnvio;
import br.com.datum.service.ClienteValidacaoService.ResultadoLinha;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** O cérebro do upload: recebe as linhas do front, valida de novo e manda só as válidas ao Python. */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final ClienteValidacaoService validacao;
    private final AppProperties props;
    private final PythonEtlService python;

    public UploadService(ClienteValidacaoService validacao, AppProperties props, PythonEtlService python) {
        this.validacao = validacao;
        this.props = props;
        this.python = python;
    }

    public Map<String, Object> receberValidadas(EnvioValidadasRequest req) {
        if (req == null || req.linhas() == null || req.linhas().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Não há linhas validadas para enviar.");
        }
        if (req.linhas().size() > props.upload().maxLinhas()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "A planilha passou do limite de " + props.upload().maxLinhas() + " linhas.");
        }
        String nome = nomeSeguro(req.arquivoNome());
        int descartadas = req.descartadas() == null ? 0 : Math.max(0, req.descartadas());

        List<ClienteDTO> brutas = new ArrayList<>(req.linhas().size());
        int posicao = 2;                                           // a linha 1 da planilha é o cabeçalho
        for (LinhaEnvio e : req.linhas()) {
            Map<String, String> campos = new LinkedHashMap<>();
            campos.put(CODIGO, e.codigoCliente());
            campos.put(NOME, e.nomeCliente());
            campos.put(CONSULTOR, e.consultor());
            campos.put(SEGMENTO, e.segmento());
            campos.put(NIVEL, e.nivelCliente());
            campos.put(FATURAMENTO, e.faturamentoAnual());
            campos.put(SERVICOS, e.servicosContratados());
            campos.put(DATA, e.dataContratacao());
            campos.put(CIDADE, e.cidade());
            campos.put(UF, e.uf());
            int numero = e.numeroLinha() == null ? posicao : e.numeroLinha();
            brutas.add(new ClienteDTO(numero, campos));
            posicao++;
        }
        return registrar(nome, brutas, descartadas);
    }

    private Map<String, Object> registrar(String nome, List<ClienteDTO> brutas, int descartadas) {
        List<ResultadoLinha> resultado = validacao.validar(brutas);      // VALIDAÇÃO NO BACKEND
        List<Map<String, Object>> validas = new ArrayList<>();
        List<String> rejeitadas = new ArrayList<>();
        int comAviso = 0;

        for (ResultadoLinha r : resultado) {
            if (!r.valida()) {                                           // linha com erro: só informa o usuário
                rejeitadas.add("Linha " + r.numero() + ": " + String.join(" | ", r.erros()));
                continue;
            }
            if (!r.avisos().isEmpty()) comAviso++;
            validas.add(paraPython(r));
        }
        if (validas.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Nenhuma linha válida para processar.");
        }

        int recebidas = resultado.size() + descartadas;
        int invalidas = rejeitadas.size() + descartadas;

        String status;
        Map<String, Object> retorno = Map.of();                          // resposta do Python (se o ETL estiver ligado)
        if (python.habilitado()) {
            retorno = python.enviar(nome, recebidas, invalidas, comAviso, validas);  // o Python valida de novo e grava
            status = "PROCESSADO";
        } else {
            log.warn("ETL desligado (ETL_ENABLED=false): upload validado, mas NÃO enviado ao Python.");
            status = "VALIDADO";
        }

        // O Python também valida: o que ele recusar entra na lista de rejeitadas e muda as contagens.
        int validasFinal = validas.size();
        int invalidasFinal = invalidas;
        if (retorno.get("linhasValidas") instanceof Number n) validasFinal = n.intValue();
        if (retorno.get("linhasInvalidas") instanceof Number n) invalidasFinal += n.intValue();
        if (retorno.get("rejeitadas") instanceof List<?> doPython) {
            for (Object o : doPython) rejeitadas.add(String.valueOf(o));
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("status", status);
        if (retorno.get("loteId") != null) saida.put("loteId", retorno.get("loteId"));
        saida.put("arquivoNome", nome);
        saida.put("totalLinhas", recebidas);
        saida.put("linhasValidas", validasFinal);
        saida.put("linhasInvalidas", invalidasFinal);
        saida.put("linhasComAviso", comAviso);
        saida.put("rejeitadas", rejeitadas);
        return saida;
    }

    /** Uma linha válida no formato que o Python recebe (faturamento como número, vazio como null). */
    private Map<String, Object> paraPython(ResultadoLinha r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numeroLinha", r.numero());
        for (String campo : TODAS) {
            String valor = vazioParaNulo(r.valores().get(campo));
            if (FATURAMENTO.equals(campo) && valor != null) m.put(campo, new BigDecimal(valor));
            else m.put(campo, valor);
        }
        return m;
    }

    private static String vazioParaNulo(String s) { return s == null || s.isBlank() ? null : s; }

    /** Tira caminhos e caracteres estranhos do nome do arquivo. */
    private static String nomeSeguro(String nome) {
        if (nome == null || nome.isBlank()) return "planilha.xlsx";
        String limpo = nome.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip();
        if (limpo.length() > 120) limpo = limpo.substring(limpo.length() - 120);
        return limpo.isEmpty() ? "planilha.xlsx" : limpo;
    }
}
