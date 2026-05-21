package com.decoraciones.seed;

import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.ImagenArticulo;
import com.decoraciones.features.imagenarticulo.ImagenArticuloRepository;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Order(5)
public class ImagenArticuloSeeder implements CommandLineRunner {

    private final ArticuloInventarioRepository articuloRepository;
    private final ImagenArticuloRepository imagenRepository;

    public ImagenArticuloSeeder(ArticuloInventarioRepository articuloRepository, ImagenArticuloRepository imagenRepository) {
        this.articuloRepository = articuloRepository;
        this.imagenRepository = imagenRepository;
    }

    @Override
    public void run(String... args) {
        seedImagenesPrincipales();
    }

    private void seedImagenesPrincipales() {
        Map<String, String> urls = Map.of(
                "sofa1", "uploads/inventario/sofa1/sofas1_123.jpg",
                "table1", "uploads/inventario/table1/table1_123.jpg",
                "table2", "uploads/inventario/table2/table2_123.jpg",
                "table3", "uploads/inventario/table3/table3_123.jpg"
        );

        for (Map.Entry<String, String> entry : urls.entrySet()) {
            String nombreArticulo = entry.getKey();
            String url = entry.getValue();

            ArticuloInventario articulo = articuloRepository.findByNombreIgnoreCase(nombreArticulo).orElse(null);
            if (articulo == null) {
                continue;
            }

            boolean yaTienePrincipal = imagenRepository
                    .findByArticuloInventarioIdAndEsPrincipalTrue(articulo.getId())
                    .isPresent();
            if (yaTienePrincipal) {
                continue;
            }

            ImagenArticulo imagen = new ImagenArticulo();
            imagen.setArticuloInventario(articulo);
            imagen.setUrl(url);
            imagen.setEsPrincipal(true);
            imagen.setOrden(1);
            imagen.setProcesadoIa(false);
            imagen.setFechaSubida(LocalDateTime.now());

            imagenRepository.save(imagen);
            System.out.println("Imagen principal creada para: " + nombreArticulo);
        }
    }
}
