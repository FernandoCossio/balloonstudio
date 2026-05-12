package com.decoraciones.features.inventario;

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
    public ResponseEntity<List<ArticuloInventarioResponse>> findAll() {
        return ResponseEntity.ok(articuloService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticuloInventarioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(articuloService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ArticuloInventarioRequest request) {
        try {
            ArticuloInventario articulo = toEntity(request);
            Set<Long> ids = request.categoriaIds() != null ? new HashSet<>(request.categoriaIds()) : null;
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(articuloService.create(articulo, ids)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ArticuloInventarioRequest request) {
        try {
            ArticuloInventario articulo = toEntity(request);
            Set<Long> ids = request.categoriaIds() != null ? new HashSet<>(request.categoriaIds()) : null;
            return ResponseEntity.ok(toResponse(articuloService.update(id, articulo, ids)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articuloService.delete(id);
        return ResponseEntity.noContent().build();
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
