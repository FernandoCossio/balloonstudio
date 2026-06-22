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
        seedImagenesAdicionales();
    }

    private void seedImagenesPrincipales() {
        Map<String, String> urls = Map.of(
                "sofa1", "uploads/inventario/sofa1/sofa1_123.jpg",
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
            imagen.setTipoVista(com.decoraciones.domain.models.TipoVistaImagen.FRONTAL);
            imagen.setOrden(1);
            imagen.setProcesadoIa(false);
            imagen.setFechaSubida(LocalDateTime.now());

            imagenRepository.save(imagen);
            System.out.println("Imagen principal creada para: " + nombreArticulo);
        }
    }

    private void seedImagenesAdicionales() {
        String[] nombres = {"sofa1", "table1", "table2", "table3"};

        for (String nombreArticulo : nombres) {
            ArticuloInventario articulo = articuloRepository.findByNombreIgnoreCase(nombreArticulo).orElse(null);
            if (articulo == null) {
                continue;
            }

            ImagenArticulo principal = imagenRepository
                    .findByArticuloInventarioIdAndEsPrincipalTrue(articulo.getId())
                    .orElse(null);
            if (principal == null) {
                continue;
            }

            java.util.List<ImagenArticulo> existing = imagenRepository.findByArticuloInventarioIdOrderByOrdenAsc(articulo.getId());
            if (existing.size() > 1) {
                continue;
            }

            String baseWebPath = principal.getUrl();
            int dotIdx = baseWebPath.lastIndexOf('.');
            if (dotIdx == -1) {
                continue;
            }

            String baseWithoutExt = baseWebPath.substring(0, dotIdx);
            String ext = baseWebPath.substring(dotIdx);

            java.nio.file.Path storageDir = java.nio.file.Paths.get("storage").toAbsolutePath();
            java.nio.file.Path srcPath = storageDir.resolve(baseWebPath);

            if (!java.nio.file.Files.exists(srcPath)) {
                System.out.println("Source physical image file does not exist: " + srcPath);
                continue;
            }

            for (int i = 1; i <= 3; i++) {
                String targetWebPath = baseWithoutExt + "_" + i + ext;
                java.nio.file.Path destPath = storageDir.resolve(targetWebPath);

                try {
                    if (!java.nio.file.Files.exists(destPath)) {
                        java.nio.file.Files.copy(srcPath, destPath, java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                        System.out.println("Copied physical file to: " + destPath);
                    }

                    ImagenArticulo extraImg = new ImagenArticulo();
                    extraImg.setArticuloInventario(articulo);
                    extraImg.setUrl(targetWebPath);
                    extraImg.setEsPrincipal(false);
                    extraImg.setOrden(i + 1);
                    extraImg.setProcesadoIa(false);
                    extraImg.setFechaSubida(LocalDateTime.now());

                    imagenRepository.save(extraImg);
                    System.out.println("Seeded additional image: " + targetWebPath);
                } catch (Exception e) {
                    System.err.println("Error copying or seeding additional image: " + e.getMessage());
                }
            }
        }
    }
}
