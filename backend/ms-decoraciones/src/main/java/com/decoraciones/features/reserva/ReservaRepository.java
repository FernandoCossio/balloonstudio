package com.decoraciones.features.reserva;

import com.decoraciones.domain.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findAllByUsuarioId(Long usuarioId);
    Optional<Reserva> findByCotizacionProyectoDisenoId(Long proyectoId);
    List<Reserva> findAllByCotizacionProyectoDisenoIdAndEstado(Long proyectoId, String estado);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reserva r JOIN FETCH r.cotizacion JOIN FETCH r.usuario JOIN FETCH r.cotizacion.proyectoDiseno " +
            "WHERE (CAST(:startDate AS timestamp) IS NULL OR r.fechaReserva >= :startDate) " +
            "  AND (CAST(:endDate AS timestamp) IS NULL OR r.fechaReserva <= :endDate) " +
            "  AND (CAST(:estado AS string) IS NULL OR LOWER(r.estado) = LOWER(CAST(:estado AS string))) " +
            "ORDER BY r.fechaReserva DESC")
    List<Reserva> buscarReporte(
            @org.springframework.data.repository.query.Param("startDate") LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDateTime endDate,
            @org.springframework.data.repository.query.Param("estado") String estado
    );
}

