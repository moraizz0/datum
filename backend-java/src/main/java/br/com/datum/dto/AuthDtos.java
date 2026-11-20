package br.com.datum.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formatos do cadastro e do login, com as validações e mensagens em português. */
public final class AuthDtos {
    private AuthDtos() {}

    public record CadastroRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome muito longo") String nome,
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail muito longo") String email,
        @NotBlank(message = "Informe a senha")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                 message = "A senha deve ter de 8 a 72 caracteres, com letras e números") String senha) {}

    public record LoginRequest(
        @NotBlank(message = "Informe o e-mail") String email,
        @NotBlank(message = "Informe a senha") String senha) {}

    public record LoginResponse(String token, String tipo, long expiraEmSegundos, UsuarioDto usuario) {}

    public record MensagemResponse(String mensagem) {}
}
