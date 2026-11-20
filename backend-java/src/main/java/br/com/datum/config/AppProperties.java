package br.com.datum.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Lê tudo que começa com "app." no application.properties. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Admin admin, Upload upload, Etl etl) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Cors(List<String> origins) {}
    public record Admin(String nome, String email, String senha) {}
    public record Upload(int maxLinhas) {}
    /** enabled=false: valida o upload mas não chama o Python (útil enquanto o ETL não existe). */
    public record Etl(boolean enabled, String url, String key) {}
}
