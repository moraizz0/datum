package br.com.datum.repository;

import br.com.datum.model.StatusUsuario;
import br.com.datum.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** O Spring cria o SQL a partir do nome de cada método. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findAllByOrderByCriadoEmDesc();
    List<Usuario> findByStatusOrderByCriadoEmDesc(StatusUsuario status);
}
