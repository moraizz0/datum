package br.com.datum.config;

import br.com.datum.model.StatusUsuario;
import br.com.datum.model.Usuario;
import br.com.datum.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Entrega o usuário ao Spring Security. Só contas APROVADAS ficam habilitadas. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repo;

    public UsuarioDetailsService(UsuarioRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = repo.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(u.getEmail())
            .password(u.getSenhaHash())                          // hash BCrypt
            .authorities("ROLE_" + u.getPerfil().name())         // ROLE_USER ou ROLE_ADMIN
            .disabled(u.getStatus() != StatusUsuario.APROVADO)   // pendente/bloqueado não passa
            .build();
    }
}
