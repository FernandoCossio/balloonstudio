package com.decoraciones.features.reserva;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse;
import com.decoraciones.domain.dtos.cotizacion.CotizacionPrevisualizarRequest;
import com.decoraciones.domain.dtos.cotizacion.ExportarPropuestaRequest;
import com.decoraciones.features.reportes.ReportesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/proyectos")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;
    private final ReportesService reportesService;

    /**
     * Previsualiza en tiempo real la cotización del lienzo sin guardar nada en la base de datos.
     */
    @PostMapping("/{proyectoId}/cotizacion/previsualizar")
    public ResponseEntity<ApiResponse<CotizacionDetalleResponse>> previsualizarCotizacion(
            @PathVariable Long proyectoId,
            @RequestBody CotizacionPrevisualizarRequest request) {

        CotizacionDetalleResponse response = cotizacionService.calcularCotizacion(
                proyectoId,
                request.elementos(),
                request.distanciaKm()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Cotización calculada correctamente."));
    }

    /**
     * Exporta la propuesta comercial del diseño en formato PDF, incluyendo la captura rasterizada del canvas.
     */
    @PostMapping("/{proyectoId}/exportar-pdf")
    public ResponseEntity<byte[]> exportarPropuestaPdf(
            @PathVariable Long proyectoId,
            @RequestBody ExportarPropuestaRequest request) {

        byte[] pdfBytes = reportesService.generarPropuestaPdf(
                proyectoId,
                request.base64Canvas(),
                request.elementos()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"propuesta-proyecto-" + proyectoId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
