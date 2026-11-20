package br.com.datum.service;

import br.com.datum.config.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Lê do banco o que o Python gravou (tabelas upload_lote e estatistica_lote) para os gráficos.
 * O Java só LÊ aqui: quem grava essas tabelas é o Python (banco.py).
 */
@Service
public class DashboardService {

    private static final String SELECT_LOTE = "SELECT id, arquivo_nome, criado_em, status, total_linhas, "
        + "linhas_validas, linhas_invalidas, linhas_com_aviso, mensagem FROM upload_lote ";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public DashboardService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Devolve { lote, geradoEm, estatisticas }. Sem loteId: o último lote PROCESSADO (ou o último enviado). */
    public Map<String, Object> obter(Long loteId) {
        List<Map<String, Object>> achados;
        if (loteId != null) {
            achados = jdbc.query(SELECT_LOTE + "WHERE id = ?", this::lerLote, loteId);
        } else {
            achados = jdbc.query(SELECT_LOTE + "WHERE status = 'PROCESSADO' ORDER BY id DESC LIMIT 1", this::lerLote);
            if (achados.isEmpty()) {
                achados = jdbc.query(SELECT_LOTE + "ORDER BY id DESC LIMIT 1", this::lerLote);
            }
        }
        if (achados.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, loteId == null
                ? "Nenhuma planilha foi processada ainda. Envie uma planilha para ver os gráficos."
                : "Lote " + loteId + " não encontrado.");
        }
        Map<String, Object> lote = achados.get(0);

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("lote", lote);
        saida.put("geradoEm", null);
        saida.put("estatisticas", null);

        List<Map<String, Object>> contas = jdbc.query(
            "SELECT gerado_em, resultado FROM estatistica_lote WHERE lote_id = ?",
            (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("geradoEm", data(rs.getTimestamp("gerado_em")));
                m.put("resultado", rs.getString("resultado"));
                return m;
            }, lote.get("id"));
        if (!contas.isEmpty()) {
            saida.put("geradoEm", contas.get(0).get("geradoEm"));
            saida.put("estatisticas", lerJson((String) contas.get(0).get("resultado")));
        }
        return saida;
    }

    /** Os últimos 20 lotes, do mais novo para o mais antigo (caixa de escolha da tela). */
    public List<Map<String, Object>> listarLotes() {
        return jdbc.query(SELECT_LOTE + "ORDER BY id DESC LIMIT 20", this::lerLote);
    }

    private Map<String, Object> lerLote(ResultSet rs, int numeroLinha) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("arquivoNome", rs.getString("arquivo_nome"));
        m.put("enviadoEm", data(rs.getTimestamp("criado_em")));
        m.put("status", rs.getString("status"));
        m.put("totalLinhas", rs.getInt("total_linhas"));
        m.put("linhasValidas", rs.getInt("linhas_validas"));
        m.put("linhasInvalidas", rs.getInt("linhas_invalidas"));
        m.put("linhasComAviso", rs.getInt("linhas_com_aviso"));
        m.put("mensagem", rs.getString("mensagem"));
        return m;
    }

    /** O banco guarda UTC; a API devolve ISO-8601 com "Z" (o navegador converte para o horário local). */
    private static Object data(Timestamp t) {
        return t == null ? null : Datas.paraInstant(t.toLocalDateTime());
    }

    private JsonNode lerJson(String texto) {
        try {
            return json.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "O resultado gravado pelo Python não é um JSON válido.");
        }
    }
}
