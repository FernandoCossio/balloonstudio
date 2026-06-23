package com.decoraciones.features.empleado;

import com.decoraciones.auth.userdetails.UsuarioPrincipal;
import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.empleado.EmpleadoRequest;
import com.decoraciones.domain.dtos.empleado.EmpleadoResponse;
import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmpleadoResponse>>> findEmpleados(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombreCompleto,asc") String[] sort
    ) {
        Sort.Direction direction = Sort.Direction.ASC;
        String property = "nombreCompleto";
        if (sort.length >= 2) {
            property = sort[0];
            if ("desc".equalsIgnoreCase(sort[1])) {
                direction = Sort.Direction.DESC;
            }
        } else if (sort.length == 1) {
            property = sort[0];
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        Page<EmpleadoResponse> response = empleadoService.findEmpleados(nombre, rol, activo, pageable)
                .map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.success(response, "Empleados obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpleadoResponse>> findById(@PathVariable Long id) {
        EmpleadoResponse response = toResponse(empleadoService.findById(id));
        return ResponseEntity.ok(ApiResponse.success(response, "Empleado obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmpleadoResponse>> create(@Valid @RequestBody EmpleadoRequest request) {
        Usuario created = empleadoService.create(
                request.nombreCompleto(),
                request.email(),
                request.telefono(),
                request.username()
        );
        EmpleadoResponse response = toResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Empleado creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpleadoResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmpleadoRequest request
    ) {
        Usuario updated = empleadoService.update(
                id,
                request.nombreCompleto(),
                request.email(),
                request.telefono(),
                request.username()
        );
        EmpleadoResponse response = toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Empleado actualizado correctamente"));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal adminPrincipal
    ) {
        empleadoService.deactivate(id, adminPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Empleado desactivado correctamente"));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        empleadoService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Empleado activado correctamente"));
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private EmpleadoResponse toResponse(Usuario u) {
        Set<String> roles = u.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());
        return new EmpleadoResponse(
                u.getId(),
                u.getUsername(),
                u.getNombreCompleto(),
                u.getEmail(),
                u.getTelefono(),
                u.getActivo(),
                roles
        );
    }
}
