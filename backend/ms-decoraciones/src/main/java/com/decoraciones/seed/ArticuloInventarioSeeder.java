package com.decoraciones.seed;

import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.Categoria;
import com.decoraciones.features.categoria.CategoriaRepository;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Order(4)
public class ArticuloInventarioSeeder implements CommandLineRunner {

    private final ArticuloInventarioRepository articuloRepository;
    private final CategoriaRepository categoriaRepository;

    public ArticuloInventarioSeeder(ArticuloInventarioRepository articuloRepository, CategoriaRepository categoriaRepository) {
        this.articuloRepository = articuloRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public void run(String... args) {
        seedArticulos();
    }

    private void seedArticulos() {
        Categoria sofas = categoriaRepository.findByNombreIgnoreCase("Sofas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Sofas' no encontrada."));
        Categoria mesas = categoriaRepository.findByNombreIgnoreCase("Mesas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Mesas' no encontrada."));

        seedArticulo("sofa1", "Sofá reutilizable para eventos", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("500.00"), new BigDecimal("30.00"), 2, Set.of(sofas));

        seedArticulo("table1", "Mesa reutilizable para eventos", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("150.00"), new BigDecimal("25.00"), 4, Set.of(mesas));

        seedArticulo("table2", "Mesa reutilizable para eventos", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("150.00"), new BigDecimal("25.00"), 4, Set.of(mesas));

        seedArticulo("table3", "Mesa reutilizable para eventos", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("150.00"), new BigDecimal("25.00"), 4, Set.of(mesas));
    }

    private void seedArticulo(String nombre,
                              String descripcion,
                              String tipoArticulo,
                              String estado,
                              BigDecimal costoAdquisicion,
                              BigDecimal porcentajeGanancia,
                              Integer stockTotal,
                              Set<Categoria> categorias) {
        if (articuloRepository.existsByNombreIgnoreCase(nombre)) {
            return;
        }

        ArticuloInventario articulo = new ArticuloInventario();
        articulo.setNombre(nombre);
        articulo.setDescripcion(descripcion);
        articulo.setTipoArticulo(tipoArticulo);
        articulo.setEstado(estado);
        articulo.setCostoAdquisicion(costoAdquisicion);
        articulo.setPorcentajeGanancia(porcentajeGanancia);
        articulo.setStockTotal(stockTotal);
        articulo.setCategorias(categorias);

        articuloRepository.save(articulo);
        System.out.println("Artículo de inventario creado: " + nombre);
    }
}
