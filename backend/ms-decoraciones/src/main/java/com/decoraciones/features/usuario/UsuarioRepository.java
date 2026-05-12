package com.decoraciones.features.usuario;

import com.decoraciones.domain.models.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<Usuario> findByUsernameIgnoreCase(String username);

	@EntityGraph(attributePaths = "roles")
	Optional<Usuario> findByEmailIgnoreCase(String email);
}
