package com.decoraciones.features.elementolienzo;

import com.decoraciones.domain.models.ElementoLienzo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ElementoLienzoRepository extends JpaRepository<ElementoLienzo, Long> {

    List<ElementoLienzo> findAllByEscenarioBaseIdOrderByZIndexAsc(Long escenarioId);

    List<ElementoLienzo> findAllByProyectoIdOrderByZIndexAsc(Long proyectoId);

    // Soft delete masivo — respeta el BaseEntity.isDeleted
    @Modifying
    @Query("UPDATE ElementoLienzo e SET e.isDeleted = true WHERE e.escenarioBase.id = :escenarioId")
    void softDeleteAllByEscenarioId(@Param("escenarioId") Long escenarioId);

    // Conteo de cantidad total de un artículo en un proyecto
    // Usado para validar contra stockTotal antes de guardar
    @Query("""
        SELECT COALESCE(SUM(e.cantidad), 0)
        FROM ElementoLienzo e
        WHERE e.articuloInventario.id = :articuloId
          AND e.proyectoId = :proyectoId
          AND e.isDeleted = false
        """)
    Integer sumCantidadByArticuloAndProyecto(
            @Param("articuloId") Long articuloId,
            @Param("proyectoId") Long proyectoId
    );
}
