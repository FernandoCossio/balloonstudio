package com.decoraciones.features.inventario;

import com.decoraciones.domain.models.BloqueoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloqueoInventarioRepository extends JpaRepository<BloqueoInventario, Long> {

    @Query("SELECT COALESCE(SUM(b.cantidad), 0) FROM BloqueoInventario b " +
           "WHERE b.articuloInventario.id = :articuloId " +
           "AND b.fechaInicio <= :fechaFin AND b.fechaFin >= :fechaInicio")
    int sumCantidadBloqueadaEnFechas(@Param("articuloId") Long articuloId,
                                     @Param("fechaInicio") LocalDate fechaInicio,
                                     @Param("fechaFin") LocalDate fechaFin);
}
