package com.decoraciones.features.imagenarticulo;

import com.decoraciones.domain.models.ImagenArticulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImagenArticuloRepository extends JpaRepository<ImagenArticulo, Long> {

    Optional<ImagenArticulo> findByArticuloInventarioIdAndEsPrincipalTrue(Long articuloId);

    List<ImagenArticulo> findByArticuloInventarioIdOrderByOrdenAsc(Long articuloId);
}
