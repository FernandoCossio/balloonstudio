package com.decoraciones.features.inventario;

import com.decoraciones.domain.models.IncidenciaArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface IncidenciaArticuloRepository extends JpaRepository<IncidenciaArticulo, Long> {

    /**
     * Calcula la suma de mermas definitivas o reparaciones activas para un rango de fechas.
     */
    @Query("""
        SELECT COALESCE(SUM(i.cantidadAfectada), 0)
        FROM IncidenciaArticulo i
        WHERE i.articuloInventario.id = :articuloId
          AND i.isDeleted = false
          AND i.estado = 'ACTIVA'
          AND (
            (i.tipo = 'MERMA_PERDIDA' AND i.fechaIncidencia <= :fechaFin)
            OR
            (i.tipo = 'REPARACION' AND i.fechaIncidencia <= :fechaFin AND (i.fechaResolucionEstimada IS NULL OR i.fechaResolucionEstimada >= :fechaInicio))
          )
        """)
    int sumCantidadIncidenciasAfectandoFechas(@Param("articuloId") Long articuloId,
                                              @Param("fechaInicio") LocalDate fechaInicio,
                                              @Param("fechaFin") LocalDate fechaFin);
}
