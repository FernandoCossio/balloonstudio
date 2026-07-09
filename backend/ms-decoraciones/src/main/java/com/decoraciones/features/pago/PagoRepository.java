package com.decoraciones.features.pago;

import com.decoraciones.domain.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findAllByReservaId(Long reservaId);
    boolean existsByReferenciaExterna(String referenciaExterna);
}
