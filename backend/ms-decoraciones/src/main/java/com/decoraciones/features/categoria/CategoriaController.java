package com.decoraciones.features.categoria;

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
    public ResponseEntity<List<CategoriaResponse>> findAll() {
        List<CategoriaResponse> response = categoriaService.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(categoriaService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CategoriaRequest request) {
        try {
            Categoria categoria = new Categoria();
            categoria.setNombre(request.nombre());
            categoria.setDescripcion(request.descripcion());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(categoriaService.create(categoria)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CategoriaRequest request) {
        try {
            Categoria categoria = new Categoria();
            categoria.setNombre(request.nombre());
            categoria.setDescripcion(request.descripcion());
            return ResponseEntity.ok(toResponse(categoriaService.update(id, categoria)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion());
    }
}
