package com.decoraciones.features.reserva;

import com.decoraciones.domain.models.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findAllByUsuarioId(Long usuarioId);
    Optional<Reserva> findByCotizacionProyectoDisenoId(Long proyectoId);
    List<Reserva> findAllByCotizacionProyectoDisenoIdAndEstado(Long proyectoId, String estado);

    @Query("SELECT r FROM Reserva r JOIN FETCH r.cotizacion JOIN FETCH r.usuario JOIN FETCH r.cotizacion.proyectoDiseno " +
            "WHERE (CAST(:startDate AS timestamp) IS NULL OR r.fechaReserva >= :startDate) " +
            "  AND (CAST(:endDate AS timestamp) IS NULL OR r.fechaReserva <= :endDate) " +
            "  AND (CAST(:estado AS string) IS NULL OR LOWER(r.estado) = LOWER(CAST(:estado AS string))) " +
            "ORDER BY r.fechaReserva DESC")
    List<Reserva> buscarReporte(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("estado") String estado
    );

    @Query("SELECT r FROM Reserva r " +
            "JOIN FETCH r.cotizacion c " +
            "JOIN FETCH c.proyectoDiseno p " +
            "JOIN FETCH p.usuario " +
            "WHERE r.id = :id")
    Optional<Reserva> findByIdWithCotizacion(@Param("id") Long id);

    @Query(value = "SELECT r FROM Reserva r " +
            "JOIN FETCH r.usuario u " +
            "JOIN FETCH r.cotizacion c " +
            "JOIN FETCH c.proyectoDiseno p " +
            "WHERE (CAST(:nombreCliente AS string) IS NULL OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombreCliente AS string), '%'))) " +
            "  AND (CAST(:estado AS string) IS NULL OR LOWER(r.estado) = LOWER(CAST(:estado AS string))) " +
            "  AND (CAST(:fechaInicio AS timestamp) IS NULL OR r.fechaReserva >= :fechaInicio) " +
            "  AND (CAST(:fechaFin AS timestamp) IS NULL OR r.fechaReserva <= :fechaFin)",
            countQuery = "SELECT COUNT(r) FROM Reserva r " +
            "WHERE (CAST(:nombreCliente AS string) IS NULL OR LOWER(r.usuario.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombreCliente AS string), '%'))) " +
            "  AND (CAST(:estado AS string) IS NULL OR LOWER(r.estado) = LOWER(CAST(:estado AS string))) " +
            "  AND (CAST(:fechaInicio AS timestamp) IS NULL OR r.fechaReserva >= :fechaInicio) " +
            "  AND (CAST(:fechaFin AS timestamp) IS NULL OR r.fechaReserva <= :fechaFin)")
    Page<Reserva> findAllWithFilters(
            @Param("nombreCliente") String nombreCliente,
            @Param("estado") String estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    @Query(value = "SELECT r FROM Reserva r " +
            "JOIN FETCH r.usuario u " +
            "JOIN FETCH r.cotizacion c " +
            "JOIN FETCH c.proyectoDiseno p " +
            "WHERE r.estado IN :estados " +
            "  AND (CAST(:nombreCliente AS string) IS NULL OR LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombreCliente AS string), '%'))) " +
            "  AND (CAST(:fechaInicio AS timestamp) IS NULL OR r.fechaReserva >= :fechaInicio) " +
            "  AND (CAST(:fechaFin AS timestamp) IS NULL OR r.fechaReserva <= :fechaFin)",
            countQuery = "SELECT COUNT(r) FROM Reserva r " +
            "WHERE r.estado IN :estados " +
            "  AND (CAST(:nombreCliente AS string) IS NULL OR LOWER(r.usuario.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombreCliente AS string), '%'))) " +
            "  AND (CAST(:fechaInicio AS timestamp) IS NULL OR r.fechaReserva >= :fechaInicio) " +
            "  AND (CAST(:fechaFin AS timestamp) IS NULL OR r.fechaReserva <= :fechaFin)")
    Page<Reserva> findActiveWithFilters(
            @Param("estados") List<String> estados,
            @Param("nombreCliente") String nombreCliente,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    @Query(value = "SELECT r FROM Reserva r " +
            "JOIN FETCH r.usuario u " +
            "JOIN FETCH r.cotizacion c " +
            "JOIN FETCH c.proyectoDiseno p " +
            "WHERE u.id = :usuarioId " +
            "  AND (CAST(:estado AS string) IS NULL OR LOWER(r.estado) = LOWER(CAST(:estado AS string))) " +
            "  AND (CAST(:fechaInicio AS timestamp) IS NULL OR r.fechaReserva >= :fechaInicio) " +
            "  AND (CAST(:fechaFin AS timestamp) IS NULL OR r.fechaReserva <= :fechaFin)",
            countQuery = "SELECT COUNT(r) FROM Reserva r " +
            "WHERE r.usuario.id = :usuarioId " +
            "  AND (CAST(:estado AS string) IS NULL OR LOWER(r.estado) = LOWER(CAST(:estado AS string))) " +
            "  AND (CAST(:fechaInicio AS timestamp) IS NULL OR r.fechaReserva >= :fechaInicio) " +
            "  AND (CAST(:fechaFin AS timestamp) IS NULL OR r.fechaReserva <= :fechaFin)")
    Page<Reserva> findByUsuarioIdWithFilters(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") String estado,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );
}
