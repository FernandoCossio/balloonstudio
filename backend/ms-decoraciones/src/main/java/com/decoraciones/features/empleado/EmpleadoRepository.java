package com.decoraciones.features.empleado;

import com.decoraciones.domain.models.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByCi(String ci);

    boolean existsByCi(String ci);

    boolean existsByEmail(String email);
}
