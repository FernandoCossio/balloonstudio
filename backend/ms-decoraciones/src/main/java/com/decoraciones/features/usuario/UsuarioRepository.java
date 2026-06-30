package com.decoraciones.features.usuario;

import com.decoraciones.domain.models.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<Usuario> findByUsernameIgnoreCase(String username);

	@EntityGraph(attributePaths = "roles")
	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
	boolean existsByUsernameIgnoreCase(String username);
	boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

	@EntityGraph(attributePaths = "roles")
	@org.springframework.data.jpa.repository.Query("SELECT u FROM Usuario u WHERE NOT EXISTS (" +
			"  SELECT r FROM u.roles r WHERE r.nombre = 'CLIENTE'" +
			") AND (CAST(:nombre AS string) IS NULL OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS string), '%')))" +
			"  AND (CAST(:rol AS string) IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE r.nombre = CAST(:rol AS string)))" +
			"  AND (CAST(:activo AS boolean) IS NULL OR u.activo = :activo)")
	org.springframework.data.domain.Page<Usuario> findEmpleados(
			@org.springframework.data.repository.query.Param("nombre") String nombre, 
			@org.springframework.data.repository.query.Param("rol") String rol, 
			@org.springframework.data.repository.query.Param("activo") Boolean activo,
			org.springframework.data.domain.Pageable pageable
	);

	@EntityGraph(attributePaths = "roles")
	@org.springframework.data.jpa.repository.Query("SELECT u FROM Usuario u " +
			"WHERE (CAST(:rol AS string) IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE LOWER(r.nombre) = LOWER(CAST(:rol AS string)))) " +
			"  AND (CAST(:activo AS boolean) IS NULL OR u.activo = :activo) " +
			"  AND (CAST(:query AS string) IS NULL OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
			"       OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
			"       OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) " +
			"  AND (CAST(:fechaInicio AS localdatetime) IS NULL OR u.createdAt >= :fechaInicio) " +
			"  AND (CAST(:fechaFin AS localdatetime) IS NULL OR u.createdAt <= :fechaFin) " +
			"ORDER BY u.nombreCompleto ASC")
	List<Usuario> buscarReporte(
			@org.springframework.data.repository.query.Param("rol") String rol,
			@org.springframework.data.repository.query.Param("activo") Boolean activo,
			@org.springframework.data.repository.query.Param("query") String query,
			@org.springframework.data.repository.query.Param("fechaInicio") java.time.LocalDateTime fechaInicio,
			@org.springframework.data.repository.query.Param("fechaFin") java.time.LocalDateTime fechaFin
	);
}
