package com.decoraciones.features.reserva;

import com.decoraciones.domain.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findAllByUsuarioId(Long usuarioId);
    Optional<Reserva> findByCotizacionProyectoDisenoId(Long proyectoId);
    List<Reserva> findAllByCotizacionProyectoDisenoIdAndEstado(Long proyectoId, String estado);
}
