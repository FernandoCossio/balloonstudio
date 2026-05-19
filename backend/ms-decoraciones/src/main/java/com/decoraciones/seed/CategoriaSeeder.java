package com.decoraciones.seed;

import com.decoraciones.domain.models.Categoria;
import com.decoraciones.features.categoria.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class CategoriaSeeder implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    public CategoriaSeeder(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public void run(String... args) {
        seedCategorias();
    }

    private void seedCategorias() {
        seedCategoria("Sofas", "Categoría para sofás y mobiliario similar");
        seedCategoria("Mesas", "Categoría para mesas y superficies de apoyo");
    }

    private void seedCategoria(String nombre, String descripcion) {
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            return;
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);
        categoriaRepository.save(categoria);
        System.out.println("Categoría creada: " + nombre);
    }
}
