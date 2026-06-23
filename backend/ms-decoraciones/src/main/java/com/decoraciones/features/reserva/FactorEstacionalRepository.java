package com.decoraciones.features.reserva;

import com.decoraciones.domain.models.FactorEstacional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FactorEstacionalRepository extends JpaRepository<FactorEstacional, Long> {
    Optional<FactorEstacional> findByMes(Integer mes);
}
