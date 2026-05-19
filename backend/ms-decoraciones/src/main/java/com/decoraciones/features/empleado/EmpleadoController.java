package com.decoraciones.features.empleado;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.empleado.EmpleadoRequest;
import com.decoraciones.domain.dtos.empleado.EmpleadoResponse;
import com.decoraciones.domain.models.Empleado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmpleadoResponse>>> findAll() {
        List<EmpleadoResponse> response = empleadoService.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Empleados obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpleadoResponse>> findById(@PathVariable Long id) {
        EmpleadoResponse response = toResponse(empleadoService.findById(id));
        return ResponseEntity.ok(ApiResponse.success(response, "Empleado obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmpleadoResponse>> create(@RequestBody EmpleadoRequest request) {
        Empleado created = empleadoService.create(toEntity(request));
        EmpleadoResponse response = toResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Empleado creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpleadoResponse>> update(@PathVariable Long id, @RequestBody EmpleadoRequest request) {
        Empleado updated = empleadoService.update(id, toEntity(request));
        EmpleadoResponse response = toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Empleado actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        empleadoService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null, "Empleado eliminado correctamente"));
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private Empleado toEntity(EmpleadoRequest r) {
        Empleado e = new Empleado();
        e.setNombre(r.nombre());
        e.setApellido(r.apellido());
        e.setCi(r.ci());
        e.setCargo(r.cargo());
        e.setTelefono(r.telefono());
        e.setEmail(r.email());
        e.setFechaContratacion(r.fechaContratacion());
        e.setActivo(r.activo() != null ? r.activo() : true);
        return e;
    }

    private EmpleadoResponse toResponse(Empleado e) {
        return new EmpleadoResponse(
                e.getId(), e.getNombre(), e.getApellido(), e.getCi(),
                e.getCargo(), e.getTelefono(), e.getEmail(),
                e.getFechaContratacion(), e.getActivo()
        );
    }
}
