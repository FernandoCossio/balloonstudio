package com.decoraciones.seed;

import com.decoraciones.domain.enums.imagenarticulo.EstadoIa;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.ImagenArticulo;
import com.decoraciones.domain.models.TipoVistaImagen;
import com.decoraciones.features.imagenarticulo.ImagenArticuloRepository;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Order(5)
public class ImagenArticuloSeeder implements CommandLineRunner {

    private final ArticuloInventarioRepository articuloRepository;
    private final ImagenArticuloRepository imagenRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ImagenArticuloSeeder(ArticuloInventarioRepository articuloRepository, ImagenArticuloRepository imagenRepository) {
        this.articuloRepository = articuloRepository;
        this.imagenRepository = imagenRepository;
    }

    private static class ArticuloImagenMap {
        String articuloNombre;
        String[] archivos;
        TipoVistaImagen[] vistas;

        ArticuloImagenMap(String name, String[] files, TipoVistaImagen[] views) {
            this.articuloNombre = name;
            this.archivos = files;
            this.vistas = views;
        }
    }

    @Override
    public void run(String... args) {
        if (imagenRepository.count() > 0) {
            System.out.println("ImagenArticuloSeeder: Ya existen imágenes de artículos en la base de datos. Omitiendo seed.");
            return;
        }
        seedImagenes();
    }

    private void seedImagenes() {
        List<ArticuloImagenMap> mappings = new java.util.ArrayList<>();

        // globos-1 to globos-6
        for (int i = 1; i <= 6; i++) {
            mappings.add(new ArticuloImagenMap("globos-" + i,
                new String[]{"globos-" + i + "-1.png"},
                new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        }

        // arco-globo-1 to arco-globo-3
        for (int i = 1; i <= 3; i++) {
            mappings.add(new ArticuloImagenMap("arco-globo-" + i,
                new String[]{"arco-globo-" + i + "-1.png"},
                new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        }

        // muro-globo-1
        mappings.add(new ArticuloImagenMap("muro-globo-1",
            new String[]{"muro-globo-1-1.png"},
            new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // arco1, arco2
        mappings.add(new ArticuloImagenMap("arco1", new String[]{"arco1-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        mappings.add(new ArticuloImagenMap("arco2", new String[]{"arco2-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // carpa-1 to carpa-3
        for (int i = 1; i <= 3; i++) {
            mappings.add(new ArticuloImagenMap("carpa-" + i,
                new String[]{"carpa-" + i + "-1.png"},
                new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        }

        // caseta1, caseta2, sombrilla1
        mappings.add(new ArticuloImagenMap("caseta1", new String[]{"caseta1-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        mappings.add(new ArticuloImagenMap("caseta2", new String[]{"caseta2-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        mappings.add(new ArticuloImagenMap("sombrilla1", new String[]{"sombrilla1-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // cinta1
        mappings.add(new ArticuloImagenMap("cinta1",
            new String[]{"cinta1-1.png", "cinta1-2.png", "cinta1-3.png", "cinta1-4.png"},
            new TipoVistaImagen[]{TipoVistaImagen.FRONTAL, TipoVistaImagen.DIAGONAL, TipoVistaImagen.LATERAL, TipoVistaImagen.TRASERO}));

        // mesa-2
        mappings.add(new ArticuloImagenMap("mesa-2", new String[]{"mesa-2-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // mesa-cuadrada-1
        mappings.add(new ArticuloImagenMap("mesa-cuadrada-1", new String[]{"mesa-cuadrada-1-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // mesa-redonda-1, mesa-redonda-2
        mappings.add(new ArticuloImagenMap("mesa-redonda-1", new String[]{"mesa-redonda-1-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        mappings.add(new ArticuloImagenMap("mesa-redonda-2", new String[]{"mesa-redonda-2-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // mesa-set-1 to mesa-set-3
        for (int i = 1; i <= 3; i++) {
            mappings.add(new ArticuloImagenMap("mesa-set-" + i,
                new String[]{"mesa-set-" + i + "-1.png"},
                new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        }

        // mesa1
        mappings.add(new ArticuloImagenMap("mesa1", new String[]{"mesa1-1.png"}, new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));

        // mesa3 (4 images)
        mappings.add(new ArticuloImagenMap("mesa3",
            new String[]{"mesa3-1.png", "mesa3-2.png", "mesa3-3.png", "mesa3-4.png"},
            new TipoVistaImagen[]{TipoVistaImagen.FRONTAL, TipoVistaImagen.DIAGONAL, TipoVistaImagen.LATERAL, TipoVistaImagen.TRASERO}));

        // silla1 to silla6
        for (int i = 1; i <= 6; i++) {
            mappings.add(new ArticuloImagenMap("silla" + i,
                new String[]{"silla" + i + "-1.png", "silla" + i + "-2.png", "silla" + i + "-3.png", "silla" + i + "-4.png"},
                new TipoVistaImagen[]{TipoVistaImagen.FRONTAL, TipoVistaImagen.DIAGONAL, TipoVistaImagen.LATERAL, TipoVistaImagen.TRASERO}));
        }

        // planta1 to planta3
        for (int i = 1; i <= 3; i++) {
            mappings.add(new ArticuloImagenMap("planta" + i,
                new String[]{"planta" + i + "-1.png"},
                new TipoVistaImagen[]{TipoVistaImagen.FRONTAL}));
        }

        Path sourceDir = Paths.get("imagenes").toAbsolutePath();

        for (ArticuloImagenMap map : mappings) {
            ArticuloInventario articulo = articuloRepository.findByNombreIgnoreCase(map.articuloNombre).orElse(null);
            if (articulo == null) {
                System.out.println("ImagenArticuloSeeder: No se encontró el artículo " + map.articuloNombre);
                continue;
            }

            Path destDir = Paths.get(uploadDir, "articulos", String.valueOf(articulo.getId()));
            try {
                Files.createDirectories(destDir);
            } catch (IOException e) {
                System.err.println("ImagenArticuloSeeder: Error al crear directorio " + destDir + ": " + e.getMessage());
                continue;
            }

            for (int i = 0; i < map.archivos.length; i++) {
                String filename = map.archivos[i];
                Path srcFile = sourceDir.resolve(filename);
                if (!Files.exists(srcFile)) {
                    System.out.println("ImagenArticuloSeeder: Archivo de origen no encontrado: " + srcFile);
                    continue;
                }

                Path destFile = destDir.resolve(filename);
                try {
                    Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("ImagenArticuloSeeder: Error al copiar " + filename + " a " + destFile + ": " + e.getMessage());
                    continue;
                }

                String relativeUrl = "uploads/articulos/" + articulo.getId() + "/" + filename;

                ImagenArticulo imagen = new ImagenArticulo();
                imagen.setArticuloInventario(articulo);
                imagen.setUrl(relativeUrl);
                imagen.setEsPrincipal(i == 0);
                imagen.setTipoVista(map.vistas[i]);
                imagen.setOrden(i + 1);
                imagen.setProcesadoIa(false);
                imagen.setEstadoIa(EstadoIa.PROCESADO);
                imagen.setFechaSubida(LocalDateTime.now());

                imagenRepository.save(imagen);
                System.out.println("Imagen cargada y registrada: " + relativeUrl);
            }
        }
    }
}
