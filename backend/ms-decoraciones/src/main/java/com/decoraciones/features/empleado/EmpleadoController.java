package com.decoraciones.features.empleado;

import com.decoraciones.domain.dtos.empleado.EmpleadoRequest;
import com.decoraciones.domain.dtos.empleado.EmpleadoResponse;
import com.decoraciones.domain.models.Empleado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping
    public ResponseEntity<List<EmpleadoResponse>> findAll() {
        return ResponseEntity.ok(empleadoService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(empleadoService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody EmpleadoRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(empleadoService.create(toEntity(request))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody EmpleadoRequest request) {
        try {
            return ResponseEntity.ok(toResponse(empleadoService.update(id, toEntity(request))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        empleadoService.delete(id);
        return ResponseEntity.noContent().build();
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
