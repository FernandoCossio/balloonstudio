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

        // Globos (CONSUMIBLE)
        for (int i = 1; i <= 6; i++) {
            seedArticulo("globos-" + i, "Set de globos decorativos tipo " + i, "CONSUMIBLE", "DISPONIBLE",
                    new BigDecimal("50.00"), new BigDecimal("15.00"), 50, Set.of(globos),
                    new BigDecimal("0.5"), new BigDecimal("0.05"), 15, 1, 0,
                    new BigDecimal("0.00"), "MEDIO", null, null, new BigDecimal("0.00"));
        }
        for (int i = 1; i <= 3; i++) {
            seedArticulo("arco-globo-" + i, "Arco de globos decorativo tipo " + i, "CONSUMIBLE", "DISPONIBLE",
                    new BigDecimal("120.00"), new BigDecimal("20.00"), 50, Set.of(globos),
                    new BigDecimal("1.5"), new BigDecimal("0.15"), 20, 1, 0,
                    new BigDecimal("0.00"), "MEDIO", null, null, new BigDecimal("0.00"));
        }
        seedArticulo("muro-globo-1", "Muro de globos para fondo de fotos tipo 1", "CONSUMIBLE", "DISPONIBLE",
                new BigDecimal("180.00"), new BigDecimal("25.00"), 50, Set.of(globos),
                new BigDecimal("2.5"), new BigDecimal("0.25"), 30, 1, 0,
                new BigDecimal("0.00"), "MEDIO", null, null, new BigDecimal("0.00"));

        // Arcos (REUTILIZABLE)
        seedArticulo("arco1", "Arco decorativo estructural metálico tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("150.00"), new BigDecimal("30.00"), 50, Set.of(arcos),
                new BigDecimal("15.0"), new BigDecimal("1.20"), 30, 2, 1,
                new BigDecimal("20.00"), "ALTO", 5, 80, new BigDecimal("40.00"));
        seedArticulo("arco2", "Arco decorativo estructural metálico tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("160.00"), new BigDecimal("30.00"), 50, Set.of(arcos),
                new BigDecimal("18.0"), new BigDecimal("1.20"), 30, 2, 1,
                new BigDecimal("20.00"), "ALTO", 5, 80, new BigDecimal("40.00"));

        // Carpas (REUTILIZABLE)
        for (int i = 1; i <= 3; i++) {
            seedArticulo("carpa-" + i, "Carpa para exteriores tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("600.00"), new BigDecimal("100.00"), 50, Set.of(carpas),
                    new BigDecimal("45.0"), new BigDecimal("3.50"), 60, 2, 2,
                    new BigDecimal("50.00"), "ALTO", 8, 120, new BigDecimal("150.00"));
        }
        seedArticulo("caseta1", "Caseta decorativa tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("250.00"), new BigDecimal("40.00"), 50, Set.of(carpas),
                new BigDecimal("25.0"), new BigDecimal("2.00"), 40, 2, 1,
                new BigDecimal("30.00"), "ALTO", 6, 100, new BigDecimal("60.00"));
        seedArticulo("caseta2", "Caseta decorativa tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("280.00"), new BigDecimal("45.00"), 50, Set.of(carpas),
                new BigDecimal("28.0"), new BigDecimal("2.20"), 45, 2, 1,
                new BigDecimal("30.00"), "ALTO", 6, 100, new BigDecimal("70.00"));
        seedArticulo("sombrilla1", "Sombrilla para jardín y exteriores", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("80.00"), new BigDecimal("15.00"), 50, Set.of(carpas),
                new BigDecimal("8.0"), new BigDecimal("0.50"), 10, 1, 1,
                new BigDecimal("10.00"), "BAJO", 4, 80, new BigDecimal("15.00"));

        // Cintas (CONSUMIBLE)
        seedArticulo("cinta1", "Cinta decorativa de colores tipo 1", "CONSUMIBLE", "DISPONIBLE",
                new BigDecimal("10.00"), new BigDecimal("2.00"), 50, Set.of(cintas),
                new BigDecimal("0.1"), new BigDecimal("0.01"), 5, 1, 0,
                new BigDecimal("0.00"), "BAJO", null, null, new BigDecimal("0.00"));

        // Mesas (REUTILIZABLE)
        seedArticulo("mesa-2", "Mesa decorativa tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("110.00"), new BigDecimal("20.00"), 50, Set.of(mesas),
                new BigDecimal("12.0"), new BigDecimal("0.80"), 10, 1, 1,
                new BigDecimal("15.00"), "BAJO", 7, 150, new BigDecimal("25.00"));
        seedArticulo("mesa-cuadrada-1", "Mesa cuadrada clásica tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("90.00"), new BigDecimal("15.00"), 50, Set.of(mesas),
                new BigDecimal("10.0"), new BigDecimal("0.70"), 10, 1, 1,
                new BigDecimal("12.00"), "BAJO", 7, 150, new BigDecimal("20.00"));
        seedArticulo("mesa-redonda-1", "Mesa redonda para eventos tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("100.00"), new BigDecimal("18.00"), 50, Set.of(mesas),
                new BigDecimal("11.0"), new BigDecimal("0.80"), 10, 1, 1,
                new BigDecimal("15.00"), "BAJO", 7, 150, new BigDecimal("25.00"));
        seedArticulo("mesa-redonda-2", "Mesa redonda para eventos tipo 2", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("100.00"), new BigDecimal("18.00"), 50, Set.of(mesas),
                new BigDecimal("11.0"), new BigDecimal("0.80"), 10, 1, 1,
                new BigDecimal("15.00"), "BAJO", 7, 150, new BigDecimal("25.00"));
        for (int i = 1; i <= 3; i++) {
            seedArticulo("mesa-set-" + i, "Set de mesa decorativa tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("220.00"), new BigDecimal("35.00"), 50, Set.of(mesas),
                    new BigDecimal("20.0"), new BigDecimal("1.50"), 15, 1, 1,
                    new BigDecimal("25.00"), "MEDIO", 6, 120, new BigDecimal("50.00"));
        }
        seedArticulo("mesa1", "Mesa rústica de madera tipo 1", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("120.00"), new BigDecimal("20.00"), 50, Set.of(mesas),
                new BigDecimal("15.0"), new BigDecimal("0.90"), 10, 1, 1,
                new BigDecimal("15.00"), "BAJO", 7, 150, new BigDecimal("30.00"));
        seedArticulo("mesa3", "Mesa moderna de metal y madera tipo 3", "REUTILIZABLE", "DISPONIBLE",
                new BigDecimal("130.00"), new BigDecimal("22.00"), 10, Set.of(mesas),
                new BigDecimal("14.0"), new BigDecimal("0.85"), 10, 1, 1,
                new BigDecimal("15.00"), "BAJO", 7, 150, new BigDecimal("30.00"));

        // Sillas (REUTILIZABLE)
        for (int i = 1; i <= 6; i++) {
            seedArticulo("silla" + i, "Silla para eventos tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("25.00"), new BigDecimal("4.00"), 50, Set.of(sillas),
                    new BigDecimal("4.5"), new BigDecimal("0.15"), 2, 1, 1,
                    new BigDecimal("5.00"), "BAJO", 5, 100, new BigDecimal("10.00"));
        }

        // Plantas (REUTILIZABLE)
        for (int i = 1; i <= 3; i++) {
            seedArticulo("planta" + i, "Planta decorativa tipo " + i, "REUTILIZABLE", "DISPONIBLE",
                    new BigDecimal("45.00"), new BigDecimal("8.00"), 50, Set.of(plantas),
                    new BigDecimal("6.0"), new BigDecimal("0.50"), 5, 1, 1,
                    new BigDecimal("8.00"), "BAJO", 3, 50, new BigDecimal("5.00"));
        }
    }

    private void seedArticulo(String nombre,
                              String descripcion,
                              String tipoArticulo,
                              String estado,
                              BigDecimal costoAdquisicion,
                              BigDecimal porcentajeGanancia,
                              Integer stockTotal,
                              Set<Categoria> categorias,
                              BigDecimal pesoKg,
                              BigDecimal volumenM3,
                              Integer tiempoArmadoMin,
                              Integer diasPreparacionPrevios,
                              Integer diasLimpiezaPosteriores,
                              BigDecimal mantenimientoPromedioBs,
                              String nivelComplejidad,
                              Integer vidaUtilAnos,
                              Integer vidaUtilUsos,
                              BigDecimal valorResidual) {
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
        
        articulo.setPesoKg(pesoKg);
        articulo.setVolumenM3(volumenM3);
        articulo.setTiempoArmadoMin(tiempoArmadoMin);
        articulo.setDiasPreparacionPrevios(diasPreparacionPrevios);
        articulo.setDiasLimpiezaPosteriores(diasLimpiezaPosteriores);
        articulo.setMantenimientoPromedioBs(mantenimientoPromedioBs);
        articulo.setNivelComplejidad(nivelComplejidad);
        articulo.setVidaUtilAnos(vidaUtilAnos);
        articulo.setVidaUtilUsos(vidaUtilUsos);
        articulo.setValorResidual(valorResidual);

        articuloRepository.save(articulo);
        System.out.println("Artículo de inventario creado: " + nombre);
    }
}
