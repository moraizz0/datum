package br.com.datum.controller;

import br.com.datum.dto.AuthDtos.CadastroRequest;
import br.com.datum.dto.AuthDtos.LoginRequest;
import br.com.datum.dto.AuthDtos.LoginResponse;
import br.com.datum.dto.AuthDtos.MensagemResponse;
import br.com.datum.dto.UsuarioDto;
import br.com.datum.service.AuthService;
import br.com.datum.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A porta de entrada do login: cadastro, login, "quem sou eu" e health. */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;
    private final UsuarioService usuarios;

    public AuthController(AuthService auth, UsuarioService usuarios) {
        this.auth = auth;
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return auth.cadastrar(req);                    // cria o usuário como PENDENTE
    }

    @PostMapping("/auth/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest req) {
        return auth.entrar(req);                       // confere a senha e devolve o token JWT
    }

    @GetMapping("/auth/me")
    public UsuarioDto eu(Authentication authentication) {
        return UsuarioDto.de(usuarios.atual(authentication));
    }

    @GetMapping("/health")
    public Map<String, String> saude() { return Map.of("status", "ok"); }
}
