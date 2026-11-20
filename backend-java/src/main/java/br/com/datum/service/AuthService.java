package br.com.datum.service;

import br.com.datum.config.ApiException;
import br.com.datum.config.JwtService;
import br.com.datum.dto.AuthDtos.CadastroRequest;
import br.com.datum.dto.AuthDtos.LoginRequest;
import br.com.datum.dto.AuthDtos.LoginResponse;
import br.com.datum.dto.AuthDtos.MensagemResponse;
import br.com.datum.dto.UsuarioDto;
import br.com.datum.model.PerfilUsuario;
import br.com.datum.model.StatusUsuario;
import br.com.datum.model.Usuario;
import br.com.datum.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String hashFalso;   // gasta o mesmo tempo quando o e-mail não existe

    public AuthService(UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.hashFalso = encoder.encode("senha-de-mentira-1");
    }

    public MensagemResponse cadastrar(CadastroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (req.senha().getBytes(StandardCharsets.UTF_8).length > 72) {          // limite do BCrypt
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha é longa demais (máximo de 72 bytes).");
        }
        if (repo.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um cadastro com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(req.senha()));                             // CRIPTOGRAFADA, nunca pura
        u.setPerfil(PerfilUsuario.USER);
        u.setStatus(StatusUsuario.PENDENTE);                                     // espera o admin liberar
        u.setCriadoEm(Datas.agoraUtc());
        repo.save(u);
        return new MensagemResponse("Cadastro recebido! Você poderá entrar assim que o administrador liberar seu acesso.");
    }

    public LoginResponse entrar(LoginRequest req) {
        Usuario u = repo.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        // Senha longa demais para o BCrypt: já é inválida (e evita erro interno).
        boolean tamanhoOk = req.senha().getBytes(StandardCharsets.UTF_8).length <= 72;
        // A senha é conferida antes do status, para não revelar quais e-mails existem.
        // Se o e-mail não existe, confere contra um hash falso para gastar o mesmo tempo.
        boolean senhaOk = tamanhoOk && encoder.matches(req.senha(), u != null ? u.getSenhaHash() : hashFalso);
        if (u == null || !senhaOk) throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        if (u.getStatus() == StatusUsuario.PENDENTE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seu cadastro ainda aguarda a liberação do administrador.");
        }
        if (u.getStatus() == StatusUsuario.BLOQUEADO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seu acesso está bloqueado. Fale com o administrador.");
        }
        return new LoginResponse(jwt.gerar(u), "Bearer", jwt.getExpiracaoSegundos(), UsuarioDto.de(u));
    }
}
