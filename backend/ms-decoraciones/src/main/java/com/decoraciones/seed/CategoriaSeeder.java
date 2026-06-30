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
        seedCategoria("Globos", "Categoría para globos, arcos de globos y muros de globos");
        seedCategoria("Arcos", "Categoría para arcos y estructuras decorativas");
        seedCategoria("Carpas", "Categoría para carpas, casetas y sombrillas");
        seedCategoria("Mesas", "Categoría para mesas y superficies de apoyo");
        seedCategoria("Sillas", "Categoría para sillas y asientos");
        seedCategoria("Plantas", "Categoría para elementos decorativos de plantas");
        seedCategoria("Cintas", "Categoría para decoraciones con cintas");
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
