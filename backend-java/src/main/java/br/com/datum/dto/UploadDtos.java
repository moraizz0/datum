package br.com.datum.dto;

import java.util.List;

public final class UploadDtos {
    private UploadDtos() {}

    /** Corpo do POST /api/uploads/validadas: só as linhas que o front (Pinia) aprovou. */
    public record EnvioValidadasRequest(String arquivoNome, Integer descartadas, List<LinhaEnvio> linhas) {}

    /** Uma linha já conferida pelo front (todos os campos chegam como texto). */
    public record LinhaEnvio(Integer numeroLinha, String codigoCliente, String nomeCliente,
                             String consultor, String segmento, String nivelCliente,
                             String faturamentoAnual, String servicosContratados,
                             String dataContratacao, String cidade, String uf) {}
}
