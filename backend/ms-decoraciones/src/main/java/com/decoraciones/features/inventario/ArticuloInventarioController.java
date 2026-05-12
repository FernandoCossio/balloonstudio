package com.decoraciones.features.inventario;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioRequest;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioResponse;
import com.decoraciones.domain.dtos.categoria.CategoriaResponse;
import com.decoraciones.domain.models.ArticuloInventario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/inventario")
public class ArticuloInventarioController {

    private final ArticuloInventarioService articuloService;

    public ArticuloInventarioController(ArticuloInventarioService articuloService) {
        this.articuloService = articuloService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticuloInventarioResponse>>> findAll() {
        List<ArticuloInventarioResponse> response = articuloService.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Artículos de inventario obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticuloInventarioResponse>> findById(@PathVariable Long id) {
        ArticuloInventarioResponse response = toResponse(articuloService.findById(id));
        return ResponseEntity.ok(ApiResponse.success(response, "Artículo de inventario obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticuloInventarioResponse>> create(@RequestBody ArticuloInventarioRequest request) {
        ArticuloInventario articulo = toEntity(request);
        Set<Long> ids = request.categoriaIds() != null ? new HashSet<>(request.categoriaIds()) : null;

        ArticuloInventario created = articuloService.create(articulo, ids);
        ArticuloInventarioResponse response = toResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Artículo de inventario creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticuloInventarioResponse>> update(@PathVariable Long id, @RequestBody ArticuloInventarioRequest request) {
        ArticuloInventario articulo = toEntity(request);
        Set<Long> ids = request.categoriaIds() != null ? new HashSet<>(request.categoriaIds()) : null;

        ArticuloInventario updated = articuloService.update(id, articulo, ids);
        ArticuloInventarioResponse response = toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Artículo de inventario actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        articuloService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null, "Artículo de inventario eliminado correctamente"));
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private ArticuloInventario toEntity(ArticuloInventarioRequest r) {
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

    private ArticuloInventarioResponse toResponse(ArticuloInventario a) {
        List<CategoriaResponse> cats = a.getCategorias() == null ? List.of() :
                a.getCategorias().stream()
                        .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion()))
                        .toList();
        return new ArticuloInventarioResponse(
                a.getId(), a.getNombre(), a.getDescripcion(), a.getTipoArticulo(), a.getEstado(),
                a.getCostoAdquisicion(), a.getPorcentajeGanancia(), a.getValorResidual(),
                a.getVidaUtilAnos(), a.getVidaUtilUsos(), a.getStockTotal(),
                a.getPesoKg(), a.getVolumenM3(), a.getTiempoArmadoMin(),
                a.getDiasPreparacionPrevios(), a.getDiasLimpiezaPosteriores(),
                a.getMantenimientoPromedioBs(), a.getNivelComplejidad(), a.getEmbeddingVisual(),
                cats
        );
    }
}
