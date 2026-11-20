package br.com.datum.service;

import br.com.datum.config.ApiException;
import br.com.datum.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * A ponte com o Python (FastAPI): faz POST /etl com as linhas que passaram na validação do backend.
 * O Python valida de novo, limpa, calcula e GRAVA no banco. O Java não grava nada do upload.
 */
@Component
public class PythonEtlService {

    private final AppProperties.Etl etl;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PythonEtlService(AppProperties props, ObjectMapper json) {
        this.etl = props.etl();
        this.json = json;
    }

    public boolean habilitado() { return etl != null && etl.enabled(); }

    /**
     * Envia as linhas e devolve o JSON de resposta do Python:
     * {ok, loteId, linhasValidas, linhasInvalidas, rejeitadas[], avisos[]}.
     */
    public Map<String, Object> enviar(String arquivoNome, int recebidas, int invalidas, int comAviso,
                                      List<Map<String, Object>> linhas) {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("arquivoNome", arquivoNome);
        pedido.put("recebidas", recebidas);
        pedido.put("invalidas", invalidas);
        pedido.put("comAviso", comAviso);
        pedido.put("linhas", linhas);
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(etl.url()))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(pedido)));
            if (etl.key() != null && !etl.key().isBlank()) req.header("X-ETL-Key", etl.key());

            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resp.statusCode();
            if (status == 422) {                                    // o Python recusou todas as linhas
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Nenhuma linha passou na validação do Python. " + motivos(resp.body()));
            }
            if (status == 401) {                                    // chave diferente nos dois lados
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "O Python recusou a chave. Confira se ETL_KEY (backend) é igual a ETL_API_KEY (Python).");
            }
            if (status / 100 != 2) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "O serviço Python respondeu com erro (" + status + ").");
            }
            return lerResposta(resp.body());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível montar os dados para o Python.");
        } catch (IOException e) {                                   // Python desligado ou inacessível
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível falar com o serviço Python (ele está ligado? uvicorn api:app --port 8000).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "A chamada ao serviço Python foi interrompida.");
        }
    }

    /** Resposta 200 do Python como mapa (se não for um JSON válido, devolve vazio). */
    private Map<String, Object> lerResposta(String corpo) {
        try {
            return json.readValue(corpo, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /** As primeiras rejeições que o Python devolveu no 422, em uma frase só. */
    private String motivos(String corpo) {
        try {
            JsonNode lista = json.readTree(corpo).path("detail").path("rejeitadas");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lista.size() && i < 3; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(lista.get(i).asText());
            }
            return sb.toString();
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
