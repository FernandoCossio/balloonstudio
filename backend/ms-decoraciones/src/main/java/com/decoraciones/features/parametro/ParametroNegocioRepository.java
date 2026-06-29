package com.decoraciones.features.parametro;

import com.decoraciones.domain.models.ParametroNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametroNegocioRepository extends JpaRepository<ParametroNegocio, Long> {
}
