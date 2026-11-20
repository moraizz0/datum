package br.com.datum.dto;

import br.com.datum.model.Usuario;
import br.com.datum.service.Datas;
import java.time.Instant;

/** O usuário que sai na resposta da API (nunca inclui a senha). */
public record UsuarioDto(Long id, String nome, String email, String perfil, String status, Instant criadoEm) {
    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(),
            u.getPerfil().name(), u.getStatus().name(), Datas.paraInstant(u.getCriadoEm()));
    }
}
