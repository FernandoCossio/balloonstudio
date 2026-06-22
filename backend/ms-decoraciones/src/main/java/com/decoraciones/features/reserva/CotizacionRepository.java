package com.decoraciones.features.reserva;

import com.decoraciones.domain.models.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    Optional<Cotizacion> findFirstByProyectoDisenoIdOrderByVersionDesc(Long proyectoId);
}
