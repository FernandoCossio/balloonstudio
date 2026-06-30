package com.decoraciones.features.reportes;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.models.Rol;
import com.decoraciones.domain.models.Reserva;
import com.decoraciones.domain.models.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReportesController {

    private final ReportesService reportesService;

    public record ReporteVentasDatosResponse(
            Long id,
            String clienteNombre,
            LocalDateTime fechaReserva,
            BigDecimal total,
            String estado
    ) {}

    public record ReporteUsuariosDatosResponse(
            Long id,
            String nombreCompleto,
            String username,
            String email,
            Set<String> roles,
            Boolean activo
    ) {}

    @GetMapping("/ventas/datos")
    public ResponseEntity<ApiResponse<List<ReporteVentasDatosResponse>>> getReporteVentasDatos(
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "estado", required = false) String estado) {

        LocalDateTime startDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime endDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        List<Reserva> reservas = reportesService.buscarReservas(startDateTime, endDateTime, estado);

        List<ReporteVentasDatosResponse> data = reservas.stream().map(r -> new ReporteVentasDatosResponse(
                r.getId(),
                r.getUsuario() != null ? r.getUsuario().getNombreCompleto() : "Cliente Desconocido",
                r.getFechaReserva(),
                r.getCotizacion() != null ? r.getCotizacion().getTotal() : BigDecimal.ZERO,
                r.getEstado()
        )).toList();

        return ResponseEntity.ok(ApiResponse.success(data, "Datos del reporte de ventas obtenidos correctamente."));
    }

    @GetMapping("/usuarios/datos")
    public ResponseEntity<ApiResponse<List<ReporteUsuariosDatosResponse>>> getReporteUsuariosDatos(
            @RequestParam(value = "rol", required = false) String rol,
            @RequestParam(value = "activo", required = false) Boolean activo,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        LocalDateTime startDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime endDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        List<Usuario> usuarios = reportesService.buscarUsuarios(rol, activo, query, startDateTime, endDateTime);

        List<ReporteUsuariosDatosResponse> data = usuarios.stream().map(u -> new ReporteUsuariosDatosResponse(
                u.getId(),
                u.getNombreCompleto(),
                u.getUsername(),
                u.getEmail(),
                u.getRoles() != null ? u.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet()) : Set.of(),
                u.getActivo()
        )).toList();

        return ResponseEntity.ok(ApiResponse.success(data, "Datos del reporte de usuarios obtenidos correctamente."));
    }

    @GetMapping("/ventas")
    public ResponseEntity<byte[]> exportarReporteVentas(
            @RequestParam(value = "format", defaultValue = "pdf") String format,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "estado", required = false) String estado) {

        LocalDateTime startDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime endDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

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
            @RequestParam(value = "activo", required = false) Boolean activo,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        LocalDateTime startDateTime = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime endDateTime = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        if (format.equalsIgnoreCase("excel")) {
            byte[] excelBytes = reportesService.generarUsuariosExcel(rol, activo, query, startDateTime, endDateTime);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-usuarios.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } else {
            byte[] pdfBytes = reportesService.generarUsuariosPdf(rol, activo, query, startDateTime, endDateTime);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-usuarios.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
    }

    @GetMapping("/ventas/{reservaId}/propuesta-pdf")
    public ResponseEntity<byte[]> exportarPropuestaPorReserva(@PathVariable Long reservaId) {
        byte[] pdfBytes = reportesService.generarPropuestaPdfPorReserva(reservaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"propuesta-reserva-" + reservaId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

