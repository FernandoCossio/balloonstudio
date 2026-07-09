package com.decoraciones.features.configuracion;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.models.Configuracion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/configuraciones")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Configuracion>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(configuracionService.findAll(), "Configuraciones obtenidas correctamente"));
    }

    @GetMapping("/{clave}")
    public ResponseEntity<ApiResponse<Configuracion>> findByClave(@PathVariable String clave) {
        return ResponseEntity.ok(ApiResponse.success(configuracionService.findByClave(clave), "Configuración obtenida correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Configuracion>> create(@RequestBody Configuracion configuracion) {
        return ResponseEntity.ok(ApiResponse.success(configuracionService.create(configuracion), "Configuración creada correctamente"));
    }

    @PutMapping("/{clave}")
    public ResponseEntity<ApiResponse<Configuracion>> update(
            @PathVariable String clave,
            @RequestParam String valor,
            @RequestParam(required = false) String descripcion
    ) {
        return ResponseEntity.ok(ApiResponse.success(configuracionService.update(clave, valor, descripcion), "Configuración actualizada correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        configuracionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Configuración eliminada correctamente"));
    }
}
