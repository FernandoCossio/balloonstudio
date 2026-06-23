package com.decoraciones.features.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReportesController {

    private final ReportesService reportesService;

    @GetMapping("/ventas")
    public ResponseEntity<byte[]> exportarReporteVentas(
            @RequestParam(value = "format", defaultValue = "pdf") String format,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "estado", required = false) String estado) {

        if (format.equalsIgnoreCase("excel")) {
            byte[] excelBytes = reportesService.generarVentasExcel(fechaInicio, fechaFin, estado);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-ventas.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } else {
            byte[] pdfBytes = reportesService.generarVentasPdf(fechaInicio, fechaFin, estado);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-ventas.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
    }

    @GetMapping("/usuarios")
    public ResponseEntity<byte[]> exportarReporteUsuarios(
            @RequestParam(value = "format", defaultValue = "pdf") String format,
            @RequestParam(value = "rol", required = false) String rol,
            @RequestParam(value = "activo", required = false) Boolean activo) {

        if (format.equalsIgnoreCase("excel")) {
            byte[] excelBytes = reportesService.generarUsuariosExcel(rol, activo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-usuarios.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } else {
            byte[] pdfBytes = reportesService.generarUsuariosPdf(rol, activo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-usuarios.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
    }
}
