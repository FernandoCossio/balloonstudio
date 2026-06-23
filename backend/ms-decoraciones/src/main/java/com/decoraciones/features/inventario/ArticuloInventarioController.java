package com.decoraciones.features.inventario;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioRequest;
import com.decoraciones.domain.dtos.articuloinventario.ArticuloInventarioResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ArticuloInventarioDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
public class ArticuloInventarioController {

    private final ArticuloInventarioService articuloService;

    public ArticuloInventarioController(ArticuloInventarioService articuloService) {
        this.articuloService = articuloService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticuloInventarioResponse>>> findAll() {
        List<ArticuloInventarioResponse> response = articuloService.findAll();
        return ResponseEntity.ok(ApiResponse.success(response, "Artículos de inventario obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticuloInventarioResponse>> findById(@PathVariable Long id) {
        ArticuloInventarioResponse response = articuloService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Artículo de inventario obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticuloInventarioResponse>> create(@RequestBody ArticuloInventarioRequest request) {
        ArticuloInventarioResponse response = articuloService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Artículo de inventario creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticuloInventarioResponse>> update(@PathVariable Long id, @RequestBody ArticuloInventarioRequest request) {
        ArticuloInventarioResponse response = articuloService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Artículo de inventario actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        articuloService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null, "Artículo de inventario eliminado correctamente"));
    }

    @GetMapping("/catalogo")
    public ResponseEntity<ApiResponse<List<ArticuloInventarioDto>>> getCatalogo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long categoriaId) {

        List<ArticuloInventarioDto> response = articuloService.getCatalogo(tipo, estado, categoriaId);
        return ResponseEntity.ok(ApiResponse.success(response, "Catálogo de artículos de inventario obtenido correctamente"));
    }
}
