package com.decoraciones.features.parametro;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.models.ParametroNegocio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parametros-negocio")
@RequiredArgsConstructor
public class ParametroNegocioController {

    private final ParametroNegocioService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ParametroNegocio>> obtenerParametros() {
        ParametroNegocio parametros = service.obtenerParametrosActivos();
        return ResponseEntity.ok(ApiResponse.success(parametros, "Parámetros de negocio obtenidos correctamente."));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ParametroNegocio>> actualizarParametros(@RequestBody ParametroNegocio nuevosParametros) {
        ParametroNegocio actualizado = service.actualizarParametros(nuevosParametros);
        return ResponseEntity.ok(ApiResponse.success(actualizado, "Parámetros de negocio actualizados correctamente."));
    }
}
