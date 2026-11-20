package br.com.datum.config;

import br.com.datum.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Cria o token JWT no login e confere se um token é válido e não expirou. */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoSegundos;

    public JwtService(AppProperties props) {
        String segredo = props.jwt() == null || props.jwt().secret() == null ? "" : props.jwt().secret();
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET precisa ter pelo menos 32 caracteres. Veja o .env.example.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoSegundos = props.jwt().expirationMinutes() * 60;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
            .subject(usuario.getEmail())                      // dono do token
            .claim("perfil", usuario.getPerfil().name())      // USER ou ADMIN (informativo)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
            .signWith(chave)                                  // ninguém consegue falsificar
            .compact();
    }

    /** E-mail do dono do token, ou vazio se o token for falso ou estiver vencido. */
    public Optional<String> emailDoToken(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(c.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpiracaoSegundos() { return expiracaoSegundos; }
}
