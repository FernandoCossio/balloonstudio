package com.decoraciones.features.proyectodiseno;

import com.decoraciones.common.decorators.CurrentUserId;
import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.proyectodiseno.*;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/proyecto-diseno")
public class ProyectoDisenoController {

    private final ProyectoDisenoService service;
    private final UsuarioRepository usuarioRepository;

    public ProyectoDisenoController(ProyectoDisenoService service,
                                    UsuarioRepository usuarioRepository) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Proyectos ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProyectoDisenoResponse>>> findAll(
            @CurrentUserId Long usuarioId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.findAllByUsuario(usuarioId),
                        "Proyectos obtenidos correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProyectoDisenoResponse>> findById(
            @PathVariable Long id,
            @CurrentUserId Long usuarioId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.findById(id, usuarioId),
                        "Proyecto obtenido correctamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProyectoDisenoResponse>> create(
            @RequestBody ProyectoDisenoRequest request,
            @CurrentUserId Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(service.create(request, usuario),
                        "Proyecto creado correctamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProyectoDisenoResponse>> update(
            @PathVariable Long id,
            @RequestBody ProyectoDisenoRequest request,
            @CurrentUserId Long usuarioId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.update(id, usuarioId, request),
                        "Proyecto actualizado correctamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @CurrentUserId Long usuarioId) {
        service.delete(id, usuarioId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "Proyecto eliminado correctamente"));
    }

    // ── Escenarios ────────────────────────────────────────────────────────────

    @GetMapping("/{proyectoId}/escenarios/{escenarioId}")
    public ResponseEntity<ApiResponse<EscenarioBaseResponse>> findEscenario(
            @PathVariable Long proyectoId,
            @PathVariable Long escenarioId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.findEscenarioById(escenarioId, proyectoId),
                        "Escenario obtenido correctamente"));
    }

    @PostMapping("/{proyectoId}/escenarios")
    public ResponseEntity<ApiResponse<EscenarioBaseResponse>> createEscenario(
            @PathVariable Long proyectoId,
            @RequestBody EscenarioBaseRequest request,
            @CurrentUserId Long usuarioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(service.createEscenario(proyectoId, usuarioId, request),
                        "Escenario creado correctamente"));
    }

    @PutMapping("/{proyectoId}/escenarios/{escenarioId}")
    public ResponseEntity<ApiResponse<EscenarioBaseResponse>> updateEscenario(
            @PathVariable Long proyectoId,
            @PathVariable Long escenarioId,
            @RequestBody EscenarioBaseRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(service.updateEscenario(escenarioId, proyectoId, request),
                        "Escenario actualizado correctamente"));
    }

    @PostMapping(value = "/{proyectoId}/escenarios/{escenarioId}/imagen",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EscenarioBaseResponse>> uploadImagen(
            @PathVariable Long proyectoId,
            @PathVariable Long escenarioId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.success(service.uploadImagenEscenario(escenarioId, proyectoId, file),
                        "Imagen del escenario subida correctamente"));
    }

    @DeleteMapping("/{proyectoId}/escenarios/{escenarioId}")
    public ResponseEntity<ApiResponse<Void>> deleteEscenario(
            @PathVariable Long proyectoId,
            @PathVariable Long escenarioId) {
        service.deleteEscenario(escenarioId, proyectoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "Escenario eliminado correctamente"));
    }

    // ── Elementos del lienzo ──────────────────────────────────────────────────

    @GetMapping("/{proyectoId}/escenarios/{escenarioId}/elementos")
    public ResponseEntity<ApiResponse<List<ElementoLienzoResponse>>> getElementos(
            @PathVariable Long escenarioId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.findElementosByEscenario(escenarioId),
                        "Elementos obtenidos correctamente"));
    }

    @PutMapping("/{proyectoId}/escenarios/{escenarioId}/elementos")
    public ResponseEntity<ApiResponse<List<ElementoLienzoResponse>>> guardarElementos(
            @PathVariable Long proyectoId,
            @PathVariable Long escenarioId,
            @RequestBody List<ElementoLienzoRequest> request) {
        return ResponseEntity.ok(
                ApiResponse.success(service.guardarElementos(escenarioId, proyectoId, request),
                        "Canvas guardado correctamente"));
    }
}
