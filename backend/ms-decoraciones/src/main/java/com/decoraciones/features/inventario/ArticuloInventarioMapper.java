package com.decoraciones.features.inventario;

import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioRequest;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioResponse;
import com.decoraciones.domain.dtos.articuloinventario.ImagenArticuloResponse;
import com.decoraciones.domain.dtos.categoria.CategoriaResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto;
import com.decoraciones.domain.dtos.proyectodiseno.ImagenArticuloDto;
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

        // Mapear todas las imagenes a su DTO con tipoVista
        List<ImagenArticuloDto> imagenesDto = imagenRepository
                .findByArticuloInventarioIdOrderByOrdenAsc(articulo.getId())
                .stream()
                .map(img -> {
                    String imgUrl = baseUrl + "/" + img.getUrl();
                    String imgThumb = baseUrl + "/thumbnails?url=" + img.getUrl();
                    String vistaStr = img.getTipoVista() != null ? img.getTipoVista().name() : null;
                    return new ImagenArticuloDto(imgUrl, imgThumb, vistaStr);
                })
                .toList();

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
                        .collect(Collectors.toSet()),
                imagenesDto
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

    public ArticuloInventarioResponse toResponse(ArticuloInventario a) {
        List<CategoriaResponse> cats = a.getCategorias() == null ? List.of() :
                a.getCategorias().stream()
                        .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion()))
                        .toList();
        List<ImagenArticuloResponse> imgs = a.getImagenes() == null ? List.of() :
                a.getImagenes().stream()
                        .map(i -> new ImagenArticuloResponse(
                                i.getId(), i.getUrl(), i.getEsPrincipal(), i.getOrden(), i.getProcesadoIa(), i.getFechaSubida()
                        ))
                        .toList();
        return new ArticuloInventarioResponse(
                a.getId(), a.getNombre(), a.getDescripcion(), a.getTipoArticulo(), a.getEstado(),
                a.getCostoAdquisicion(), a.getPorcentajeGanancia(), a.getValorResidual(),
                a.getVidaUtilAnos(), a.getVidaUtilUsos(), a.getStockTotal(),
                a.getPesoKg(), a.getVolumenM3(), a.getTiempoArmadoMin(),
                a.getDiasPreparacionPrevios(), a.getDiasLimpiezaPosteriores(),
                a.getMantenimientoPromedioBs(), a.getNivelComplejidad(), a.getEmbeddingVisual(),
                cats,
                imgs
        );
    }

    public ArticuloInventario toEntity(ArticuloInventarioRequest r) {
        ArticuloInventario a = new ArticuloInventario();
        a.setNombre(r.nombre());
        a.setDescripcion(r.descripcion());
        a.setTipoArticulo(r.tipoArticulo());
        a.setEstado(r.estado());
        a.setCostoAdquisicion(r.costoAdquisicion());
        a.setPorcentajeGanancia(r.porcentajeGanancia());
        a.setValorResidual(r.valorResidual());
        a.setVidaUtilAnos(r.vidaUtilAnos());
        a.setVidaUtilUsos(r.vidaUtilUsos());
        a.setStockTotal(r.stockTotal());
        a.setPesoKg(r.pesoKg());
        a.setVolumenM3(r.volumenM3());
        a.setTiempoArmadoMin(r.tiempoArmadoMin());
        a.setDiasPreparacionPrevios(r.diasPreparacionPrevios());
        a.setDiasLimpiezaPosteriores(r.diasLimpiezaPosteriores());
        a.setMantenimientoPromedioBs(r.mantenimientoPromedioBs());
        a.setNivelComplejidad(r.nivelComplejidad());
        a.setEmbeddingVisual(r.embeddingVisual());
        return a;
    }
}