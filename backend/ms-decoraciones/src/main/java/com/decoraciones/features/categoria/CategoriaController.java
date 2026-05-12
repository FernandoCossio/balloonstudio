package com.decoraciones.features.categoria;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.categoria.CategoriaRequest;
import com.decoraciones.domain.dtos.categoria.CategoriaResponse;
import com.decoraciones.domain.models.Categoria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> findAll() {
        List<CategoriaResponse> response = categoriaService.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Categorías obtenidas correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> findById(@PathVariable Long id) {
        CategoriaResponse response = toResponse(categoriaService.findById(id));
        return ResponseEntity.ok(ApiResponse.success(response, "Categoría obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoriaResponse>> create(@RequestBody CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.nombre());
        categoria.setDescripcion(request.descripcion());

        Categoria created = categoriaService.create(categoria);
        CategoriaResponse response = toResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Categoría creada correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> update(@PathVariable Long id, @RequestBody CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.nombre());
        categoria.setDescripcion(request.descripcion());

        Categoria updated = categoriaService.update(id, categoria);
        CategoriaResponse response = toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Categoría actualizada correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoriaService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null, "Categoría eliminada correctamente"));
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
