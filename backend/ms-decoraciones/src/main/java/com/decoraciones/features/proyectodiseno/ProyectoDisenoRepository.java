package com.decoraciones.features.proyectodiseno;

import com.decoraciones.domain.models.ProyectoDiseno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProyectoDisenoRepository extends JpaRepository<ProyectoDiseno, Long> {

    List<ProyectoDiseno> findAllByUsuarioIdOrderByFechaUltimaModificacionDesc(Long usuarioId);

    Optional<ProyectoDiseno> findByIdAndUsuarioId(Long id, Long usuarioId);

    // Verifica propiedad antes de operar — evita acceso cruzado entre usuarios
    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("SELECT p FROM ProyectoDiseno p LEFT JOIN FETCH p.escenarios WHERE p.id = :id AND p.usuario.id = :usuarioId")
    Optional<ProyectoDiseno> findByIdWithEscenarios(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
