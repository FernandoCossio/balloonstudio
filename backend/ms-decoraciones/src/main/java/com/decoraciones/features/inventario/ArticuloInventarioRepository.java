package com.decoraciones.features.inventario;

import com.decoraciones.domain.models.ArticuloInventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticuloInventarioRepository extends JpaRepository<ArticuloInventario, Long> {

    @EntityGraph(attributePaths = "categorias")
    List<ArticuloInventario> findAll();

    @EntityGraph(attributePaths = "categorias")
    Optional<ArticuloInventario> findById(Long id);
}
