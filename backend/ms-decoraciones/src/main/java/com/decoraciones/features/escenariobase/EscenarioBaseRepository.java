package com.decoraciones.features.escenariobase;

import com.decoraciones.domain.models.EscenarioBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EscenarioBaseRepository extends JpaRepository<EscenarioBase, Long> {

    List<EscenarioBase> findAllByProyectoDisenoIdOrderByIdAsc(Long proyectoDisenoId);

    Optional<EscenarioBase> findByIdAndProyectoDisenoId(Long id, Long proyectoDisenoId);

    @Query("SELECT e FROM EscenarioBase e LEFT JOIN FETCH e.elementos WHERE e.id = :id")
    Optional<EscenarioBase> findByIdWithElementos(@Param("id") Long id);
}
