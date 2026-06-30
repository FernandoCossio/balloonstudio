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
        if (articuloRepository.count() > 0) {
            System.out.println("ArticuloInventarioSeeder: Ya existen artículos en la base de datos. Omitiendo seed.");
            return;
        }
        seedArticulos();
    }

    private void seedArticulos() {
        Categoria globos = categoriaRepository.findByNombreIgnoreCase("Globos")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Globos' no encontrada."));
        Categoria arcos = categoriaRepository.findByNombreIgnoreCase("Arcos")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Arcos' no encontrada."));
        Categoria carpas = categoriaRepository.findByNombreIgnoreCase("Carpas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Carpas' no encontrada."));
        Categoria mesas = categoriaRepository.findByNombreIgnoreCase("Mesas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Mesas' no encontrada."));
        Categoria sillas = categoriaRepository.findByNombreIgnoreCase("Sillas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Sillas' no encontrada."));
        Categoria plantas = categoriaRepository.findByNombreIgnoreCase("Plantas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Plantas' no encontrada."));
        Categoria cintas = categoriaRepository.findByNombreIgnoreCase("Cintas")
                .orElseThrow(() -> new RuntimeException("Error: Categoría 'Cintas' no encontrada."));

        // Globos
        for (int i = 1; i <= 6; i++) {
            seedArticulo("globos-" + i, "Set de globos decorativos tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("50.00"), new BigDecimal("15.00"), 20, Set.of(globos));
        }
        for (int i = 1; i <= 3; i++) {
            seedArticulo("arco-globo-" + i, "Arco de globos decorativo tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("120.00"), new BigDecimal("20.00"), 5, Set.of(globos));
        }
        seedArticulo("muro-globo-1", "Muro de globos para fondo de fotos tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("180.00"), new BigDecimal("25.00"), 5, Set.of(globos));

        // Arcos
        seedArticulo("arco1", "Arco decorativo estructural metálico tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("150.00"), new BigDecimal("30.00"), 10, Set.of(arcos));
        seedArticulo("arco2", "Arco decorativo estructural metálico tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("160.00"), new BigDecimal("30.00"), 10, Set.of(arcos));

        // Carpas
        for (int i = 1; i <= 3; i++) {
            seedArticulo("carpa-" + i, "Carpa para exteriores tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("600.00"), new BigDecimal("100.00"), 4, Set.of(carpas));
        }
        seedArticulo("caseta1", "Caseta decorativa tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("250.00"), new BigDecimal("40.00"), 5, Set.of(carpas));
        seedArticulo("caseta2", "Caseta decorativa tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("280.00"), new BigDecimal("45.00"), 5, Set.of(carpas));
        seedArticulo("sombrilla1", "Sombrilla para jardín y exteriores", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("80.00"), new BigDecimal("15.00"), 8, Set.of(carpas));

        // Cintas
        seedArticulo("cinta1", "Cinta decorativa de colores tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("10.00"), new BigDecimal("2.00"), 50, Set.of(cintas));

        // Mesas
        seedArticulo("mesa-2", "Mesa decorativa tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("110.00"), new BigDecimal("20.00"), 10, Set.of(mesas));
        seedArticulo("mesa-cuadrada-1", "Mesa cuadrada clásica tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("90.00"), new BigDecimal("15.00"), 10, Set.of(mesas));
        seedArticulo("mesa-redonda-1", "Mesa redonda para eventos tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("100.00"), new BigDecimal("18.00"), 15, Set.of(mesas));
        seedArticulo("mesa-redonda-2", "Mesa redonda para eventos tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("100.00"), new BigDecimal("18.00"), 15, Set.of(mesas));
        for (int i = 1; i <= 3; i++) {
            seedArticulo("mesa-set-" + i, "Set de mesa decorativa tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("220.00"), new BigDecimal("35.00"), 5, Set.of(mesas));
        }
        seedArticulo("mesa1", "Mesa rústica de madera tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("120.00"), new BigDecimal("20.00"), 10, Set.of(mesas));
        seedArticulo("mesa3", "Mesa moderna de metal y madera tipo 3", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("130.00"), new BigDecimal("22.00"), 10, Set.of(mesas));

        // Sillas
        for (int i = 1; i <= 6; i++) {
            seedArticulo("silla" + i, "Silla para eventos tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("25.00"), new BigDecimal("4.00"), 50, Set.of(sillas));
        }

        // Plantas
        for (int i = 1; i <= 3; i++) {
            seedArticulo("planta" + i, "Planta decorativa tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("45.00"), new BigDecimal("8.00"), 15, Set.of(plantas));
        }
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
