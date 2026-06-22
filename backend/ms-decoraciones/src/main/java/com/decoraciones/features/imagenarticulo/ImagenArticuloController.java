package com.decoraciones.features.imagenarticulo;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.articuloinventario.ImagenArticuloResponse;
import com.decoraciones.domain.models.ImagenArticulo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/inventario/{articuloId}/imagenes")
public class ImagenArticuloController {

    private final ImagenArticuloService imagenService;
    private final ImagenArticuloRepository imagenRepository;

    public ImagenArticuloController(ImagenArticuloService imagenService, ImagenArticuloRepository imagenRepository) {
        this.imagenService = imagenService;
        this.imagenRepository = imagenRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<ImagenArticuloResponse>>> upload(
            @PathVariable Long articuloId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "tipoVista", required = false) String tipoVista) throws IOException {
        
        List<ImagenArticulo> creadas = imagenService.uploadImagenes(articuloId, files, tipoVista);
        List<ImagenArticuloResponse> response = creadas.stream().map(this::toResponse).toList();
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Imágenes cargadas correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImagenArticuloResponse>>> findAll(@PathVariable Long articuloId) {
        List<ImagenArticulo> imagenes = imagenRepository.findByArticuloInventarioIdOrderByOrdenAsc(articuloId);
        List<ImagenArticuloResponse> response = imagenes.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Imágenes del artículo obtenidas correctamente"));
    }

    @PatchMapping("/{imagenId}/tipo-vista")
    public ResponseEntity<ApiResponse<Void>> setTipoVista(
            @PathVariable Long articuloId,
            @PathVariable Long imagenId,
            @RequestParam("tipoVista") String tipoVista) {
        
        imagenService.setTipoVista(articuloId, imagenId, tipoVista);
        return ResponseEntity.ok(ApiResponse.success(null, "Tipo de vista de la imagen actualizado correctamente"));
    }

    @PatchMapping("/{imagenId}/principal")
    public ResponseEntity<ApiResponse<Void>> setPrincipal(
            @PathVariable Long articuloId,
            @PathVariable Long imagenId) {
        
        imagenService.setPrincipal(articuloId, imagenId);
        return ResponseEntity.ok(ApiResponse.success(null, "Imagen principal actualizada correctamente"));
    }

    @DeleteMapping("/{imagenId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long articuloId,
            @PathVariable Long imagenId) {
        
        imagenService.deleteImagen(articuloId, imagenId);
        return ResponseEntity.ok(ApiResponse.success(null, "Imagen eliminada correctamente"));
    }

    private ImagenArticuloResponse toResponse(ImagenArticulo i) {
        return new ImagenArticuloResponse(
                i.getId(),
                i.getUrl(),
                i.getEsPrincipal(),
                i.getOrden(),
                i.getProcesadoIa(),
                i.getFechaSubida(),
                i.getTipoVista() != null ? i.getTipoVista().name() : null
        );
    }
}
