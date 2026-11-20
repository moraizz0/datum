package br.com.datum.config;

import org.springframework.http.HttpStatus;

/** Erro nosso, com status HTTP. O ErroHandler transforma em {"erro": "..."}. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
