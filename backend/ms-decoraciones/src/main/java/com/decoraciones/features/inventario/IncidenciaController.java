package com.decoraciones.features.inventario;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.incidencia.IncidenciaRequest;
import com.decoraciones.domain.dtos.incidencia.IncidenciaResponse;
import com.decoraciones.domain.dtos.incidencia.SolucionarIncidenciaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    /**
     * Reportar una nueva incidencia (REPARACION o MERMA_PERDIDA).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<IncidenciaResponse>> reportarIncidencia(
            @RequestBody @Valid IncidenciaRequest request) {

        IncidenciaResponse guardada = incidenciaService.reportarIncidencia(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(guardada, "Incidencia reportada correctamente."));
    }

    /**
     * Solucionar una incidencia (libera el stock bloqueado por reparación).
     */
    @PatchMapping("/{id}/solucionar")
    public ResponseEntity<ApiResponse<IncidenciaResponse>> solucionarIncidencia(
            @PathVariable Long id,
            @RequestBody(required = false) SolucionarIncidenciaRequest request) {

        IncidenciaResponse guardada = incidenciaService.solucionarIncidencia(id, request);
        return ResponseEntity.ok(ApiResponse.success(guardada, "Incidencia solucionada. El stock vuelve a estar disponible."));
    }

    /**
     * Listar todas las incidencias.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidenciaResponse>>> listarIncidencias() {
        List<IncidenciaResponse> lista = incidenciaService.listarIncidencias();
        return ResponseEntity.ok(ApiResponse.success(lista, "Listado de incidencias obtenido."));
    }
}
