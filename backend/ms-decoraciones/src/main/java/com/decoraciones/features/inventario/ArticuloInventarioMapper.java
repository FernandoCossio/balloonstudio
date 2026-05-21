// features/inventario/ArticuloInventarioMapper.java
package com.decoraciones.features.inventario;

import com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.Categoria;
import com.decoraciones.domain.models.ImagenArticulo;
import com.decoraciones.features.imagenarticulo.ImagenArticuloRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ArticuloInventarioMapper {

    @Value("${app.base-url}")
    private String baseUrl;

    private final ImagenArticuloRepository imagenRepository;

    public ArticuloInventarioMapper(ImagenArticuloRepository imagenRepository) {
        this.imagenRepository = imagenRepository;
    }

    public ArticuloInventarioDto toDto(ArticuloInventario articulo) {
        Optional<String> urlRelativa = resolverUrlRelativa(articulo.getId());

        // URL completa para el canvas: baseUrl + "/" + url de BD
        // Ejemplo: "http://localhost:8080/uploads/inventario/silla_oak/silla_oak-123.png"
        String imagenUrl = urlRelativa
                .map(url -> baseUrl + "/" + url)
                .orElse(null);

        // Thumbnail: llama al endpoint dedicado pasando la url relativa como param
        // Ejemplo: "http://localhost:8080/api/thumbnails?url=uploads/inventario/..."
        String thumbnailUrl = urlRelativa
                .map(url -> baseUrl + "/thumbnails?url=" + url)
                .orElse(null);

        return new ArticuloInventarioDto(
                articulo.getId(),
                articulo.getNombre(),
                articulo.getDescripcion(),
                articulo.getTipoArticulo(),
                articulo.getEstado(),
                articulo.getCostoAdquisicion(),
                articulo.getPorcentajeGanancia(),
                articulo.getStockTotal(),
                imagenUrl,
                thumbnailUrl,
                articulo.getCategorias().stream()
                        .map(Categoria::getNombre)
                        .collect(Collectors.toSet())
        );
    }

    private Optional<String> resolverUrlRelativa(Long articuloId) {
        return imagenRepository
                .findByArticuloInventarioIdAndEsPrincipalTrue(articuloId)
                .map(ImagenArticulo::getUrl)
                .or(() -> {
                    List<ImagenArticulo> imagenes = imagenRepository
                            .findByArticuloInventarioIdOrderByOrdenAsc(articuloId);
                    return imagenes.isEmpty()
                            ? Optional.empty()
                            : Optional.of(imagenes.get(0).getUrl());
                });
    }
}