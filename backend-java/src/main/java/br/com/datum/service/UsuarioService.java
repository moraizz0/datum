package br.com.datum.service;

import br.com.datum.config.ApiException;
import br.com.datum.dto.UsuarioDto;
import br.com.datum.model.StatusUsuario;
import br.com.datum.model.Usuario;
import br.com.datum.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** As regras do administrador: descobre quem está logado, lista usuários e muda o status. */
@Service
public class UsuarioService {

    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo) { this.repo = repo; }

    /** O e-mail do token vira uma busca no banco. */
    public Usuario atual(Authentication authentication) {
        return repo.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Faça login novamente."));
    }

    public List<UsuarioDto> listar(StatusUsuario status) {
        List<Usuario> lista = status == null
            ? repo.findAllByOrderByCriadoEmDesc()
            : repo.findByStatusOrderByCriadoEmDesc(status);
        return lista.stream().map(UsuarioDto::de).toList();                      // sem senha
    }

    public UsuarioDto alterarStatus(Long id, StatusUsuario novoStatus, Usuario quemAltera) {
        Usuario alvo = repo.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (alvo.getId().equals(quemAltera.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Você não pode alterar o seu próprio acesso.");
        }
        alvo.setStatus(novoStatus);
        return UsuarioDto.de(repo.save(alvo));
    }
}
