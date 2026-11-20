package br.com.datum.config;

import br.com.datum.model.PerfilUsuario;
import br.com.datum.model.StatusUsuario;
import br.com.datum.model.Usuario;
import br.com.datum.repository.UsuarioRepository;
import br.com.datum.service.Datas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o primeiro ADMIN (e-mail e senha vêm do .env). Ele aprova os demais usuários. */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminInicial(UsuarioRepository repo, PasswordEncoder encoder, AppProperties props) {
        this.repo = repo;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        var cfg = props.admin();
        if (cfg == null || cfg.email() == null || cfg.email().isBlank()) return;
        if (cfg.senha() == null || cfg.senha().isBlank()) {
            log.warn("ADMIN_PASSWORD não definida: o administrador inicial NÃO foi criado.");
            return;
        }
        String email = cfg.email().trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(email)) return;           // já existe: não cria de novo

        Usuario admin = new Usuario();
        admin.setNome(cfg.nome() == null || cfg.nome().isBlank() ? "Administrador" : cfg.nome().trim());
        admin.setEmail(email);
        admin.setSenhaHash(encoder.encode(cfg.senha()));
        admin.setPerfil(PerfilUsuario.ADMIN);
        admin.setStatus(StatusUsuario.APROVADO);                   // já nasce aprovado
        admin.setCriadoEm(Datas.agoraUtc());
        repo.save(admin);
        log.warn("Administrador inicial criado: {}", email);
    }
}
