package com.decoraciones.features.reportes;

import com.decoraciones.common.errors.ProyectoDisenoNoEncontradoException;
import com.decoraciones.domain.dtos.cotizacion.CotizacionArticuloDetalle;
import com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest;
import com.decoraciones.domain.models.ProyectoDiseno;
import com.decoraciones.domain.models.Reserva;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.elementolienzo.ElementoLienzoRepository;
import com.decoraciones.features.proyectodiseno.ProyectoDisenoRepository;
import com.decoraciones.features.reserva.CotizacionService;
import com.decoraciones.features.reserva.ReservaRepository;
import com.decoraciones.features.usuario.UsuarioRepository;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportesService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoDisenoRepository proyectoRepository;
    private final CotizacionService cotizacionService;
    private final ElementoLienzoRepository elementoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Propuesta Comercial PDF ───────────────────────────────────────────────

    public byte[] generarPropuestaPdf(Long proyectoId, String base64Canvas, List<ElementoLienzoRequest> items) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        List<ElementoLienzoRequest> finalItems = items;
        if (finalItems == null || finalItems.isEmpty()) {
            List<com.decoraciones.domain.models.ElementoLienzo> elementosDb = elementoRepository.findAllByProyectoIdOrderByZIndexAsc(proyectoId);
            finalItems = elementosDb.stream().map(el -> new ElementoLienzoRequest(
                    el.getArticuloInventario().getId(),
                    el.getCantidad(),
                    el.getPosX(), el.getPosY(),
                    el.getWidth(), el.getHeight(),
                    el.getScaleX(), el.getScaleY(),
                    el.getRotacionDeg(), el.getOpacity(),
                    el.getZIndex(), el.getLayer(),
                    el.getVistaActual()
            )).toList();
        }

        CotizacionDetalleResponse cot = cotizacionService.calcularCotizacion(proyectoId, finalItems, null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Estilos de fuentes
            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, java.awt.Color.BLUE);
            Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.BLACK);
            Font textFont = new Font(Font.HELVETICA, 10, Font.NORMAL, java.awt.Color.BLACK);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE);

            // Membrete / Cabecera
            Paragraph header = new Paragraph("BALLOON STUDIO", new Font(Font.HELVETICA, 24, Font.BOLD, new java.awt.Color(138, 43, 226)));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subtitle = new Paragraph("Propuesta Comercial y Cotización de Diseño", new Font(Font.HELVETICA, 12, Font.ITALIC, java.awt.Color.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Información del Proyecto
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);

            addCell(infoTable, "Proyecto:", labelFont);
            addCell(infoTable, proyecto.getNombre(), textFont);
            addCell(infoTable, "Cliente:", labelFont);
            addCell(infoTable, proyecto.getUsuario().getNombreCompleto() + " (" + proyecto.getUsuario().getEmail() + ")", textFont);
            addCell(infoTable, "Fecha Evento:", labelFont);
            addCell(infoTable, proyecto.getFechaEvento() != null ? proyecto.getFechaEvento().format(DATE_FORMATTER) : "No definida", textFont);
            addCell(infoTable, "Ubicación:", labelFont);
            addCell(infoTable, proyecto.getLugarEvento() != null ? proyecto.getLugarEvento() : "No especificado", textFont);
            addCell(infoTable, "Distancia Estimada:", labelFont);
            addCell(infoTable, (proyecto.getDistanciaKm() != null ? proyecto.getDistanciaKm() : 10.0) + " km", textFont);

            document.add(infoTable);

            // Imagen del Canvas (si se provee)
            if (base64Canvas != null && base64Canvas.contains(",")) {
                try {
                    String base64ImageBytes = base64Canvas.substring(base64Canvas.indexOf(",") + 1);
                    byte[] imageBytes = Base64.getDecoder().decode(base64ImageBytes);
                    Image image = Image.getInstance(imageBytes);
                    image.setAlignment(Image.ALIGN_CENTER);
                    image.scaleToFit(450, 300);
                    image.setSpacingAfter(20);
                    document.add(image);
                } catch (Exception e) {
                    log.error("Error al renderizar la imagen del canvas en el PDF", e);
                }
            }

            // Tabla de Artículos
            document.add(new Paragraph("Desglose de Artículos", sectionFont));
            document.add(new Paragraph(" ", textFont));

            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{40, 15, 15, 15, 15});
            itemsTable.setSpacingAfter(15);

            // Headers
            java.awt.Color primaryColor = new java.awt.Color(138, 43, 226);
            addCellWithBackground(itemsTable, "Artículo", headerFont, primaryColor);
            addCellWithBackground(itemsTable, "Tipo", headerFont, primaryColor);
            addCellWithBackground(itemsTable, "Cant.", headerFont, primaryColor);
            addCellWithBackground(itemsTable, "P. Unit (Bs.)", headerFont, primaryColor);
            addCellWithBackground(itemsTable, "Total (Bs.)", headerFont, primaryColor);

            for (CotizacionArticuloDetalle item : cot.desgloseArticulos()) {
                addCell(itemsTable, item.nombre(), textFont);
                addCell(itemsTable, item.tipoArticulo(), textFont);
                addCell(itemsTable, String.valueOf(item.cantidad()), textFont);
                addCell(itemsTable, item.precioUnitario().toString() + " Bs.", textFont);
                addCell(itemsTable, item.precioTotal().toString() + " Bs.", textFont);
            }
            document.add(itemsTable);

            // Resumen de Costos y Totales
            document.add(new Paragraph("Resumen Económico", sectionFont));
            document.add(new Paragraph(" ", textFont));

            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addCell(totalsTable, "Subtotal Mobiliario:", labelFont);
            addCell(totalsTable, cot.costoArticulos().toString() + " Bs.", textFont);

            addCell(totalsTable, "Envío (Flete):", labelFont);
            addCell(totalsTable, cot.costoFlete().toString() + " Bs.", textFont);

            addCell(totalsTable, "Instalación (Armado):", labelFont);
            addCell(totalsTable, cot.costoArmado().toString() + " Bs.", textFont);

            addCell(totalsTable, "Tasa Overhead (10%):", labelFont);
            addCell(totalsTable, cot.subtotalConOverhead().subtract(cot.subtotal()).setScale(2, RoundingMode.HALF_UP).toString() + " Bs.", textFont);

            addCell(totalsTable, "Ajuste Estacional (Factor):", labelFont);
            addCell(totalsTable, cot.factorEstacionalAplicado().toString() + "x", textFont);

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL PROPUESTA:", new Font(Font.HELVETICA, 11, Font.BOLD, java.awt.Color.WHITE)));
            totalLabelCell.setBackgroundColor(primaryColor);
            totalLabelCell.setPadding(6);
            totalsTable.addCell(totalLabelCell);

            PdfPCell totalValCell = new PdfPCell(new Phrase(cot.total().toString() + " Bs.", new Font(Font.HELVETICA, 11, Font.BOLD, java.awt.Color.WHITE)));
            totalValCell.setBackgroundColor(primaryColor);
            totalValCell.setPadding(6);
            totalsTable.addCell(totalValCell);

            document.add(totalsTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de la propuesta comercial", e);
            throw new RuntimeException("Error al generar PDF de la propuesta comercial: " + e.getMessage(), e);
        }
    }

    // ── Reporte de Ventas PDF ─────────────────────────────────────────────────

    public byte[] generarVentasPdf(LocalDate inicio, LocalDate fin, String estado) {
        LocalDateTime startDateTime = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime endDateTime = fin != null ? fin.atTime(23, 59, 59) : null;
        List<Reserva> reservas = reservaRepository.buscarReporte(startDateTime, endDateTime, estado);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font infoFont = new Font(Font.HELVETICA, 10, Font.ITALIC, java.awt.Color.GRAY);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE);
            Font textFont = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.BLACK);

            Paragraph title = new Paragraph("Reporte de Ventas y Reservas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph info = new Paragraph("Filtros: Rango [" + (inicio != null ? inicio : "Inicio") + " al " + (fin != null ? fin : "Fin") + "] | Estado: " + (estado != null ? estado : "TODOS"), infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(20);
            document.add(info);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{10, 20, 20, 15, 12, 11, 12});

            java.awt.Color headerColor = new java.awt.Color(70, 130, 180);
            addCellWithBackground(table, "ID", headerFont, headerColor);
            addCellWithBackground(table, "Cliente", headerFont, headerColor);
            addCellWithBackground(table, "Proyecto", headerFont, headerColor);
            addCellWithBackground(table, "Fecha Reserva", headerFont, headerColor);
            addCellWithBackground(table, "Estado", headerFont, headerColor);
            addCellWithBackground(table, "Anticipo", headerFont, headerColor);
            addCellWithBackground(table, "Total", headerFont, headerColor);

            BigDecimal totalVendido = BigDecimal.ZERO;
            BigDecimal totalAnticipos = BigDecimal.ZERO;

            for (Reserva r : reservas) {
                table.addCell(new Phrase(r.getId().toString(), textFont));
                table.addCell(new Phrase(r.getUsuario().getNombreCompleto(), textFont));
                table.addCell(new Phrase(r.getCotizacion().getProyectoDiseno().getNombre(), textFont));
                table.addCell(new Phrase(r.getFechaReserva().format(TIME_FORMATTER), textFont));
                table.addCell(new Phrase(r.getEstado(), textFont));
                table.addCell(new Phrase(r.getMontoAnticipo() != null ? r.getMontoAnticipo().toString() + " Bs." : "0 Bs.", textFont));
                table.addCell(new Phrase(r.getCotizacion().getTotal() != null ? r.getCotizacion().getTotal().toString() + " Bs." : "0 Bs.", textFont));

                if (r.getCotizacion().getTotal() != null) {
                    totalVendido = totalVendido.add(r.getCotizacion().getTotal());
                }
                if (r.getMontoAnticipo() != null) {
                    totalAnticipos = totalAnticipos.add(r.getMontoAnticipo());
                }
            }

            document.add(table);

            Paragraph totals = new Paragraph("\nTotal Reservas: " + reservas.size() +
                    "\nTotal Recaudado (Anticipos): " + totalAnticipos + " Bs." +
                    "\nValor Total Proyectado: " + totalVendido + " Bs.",
                    new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.DARK_GRAY));
            totals.setAlignment(Element.ALIGN_RIGHT);
            document.add(totals);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de ventas", e);
            throw new RuntimeException("Error al generar PDF de ventas: " + e.getMessage(), e);
        }
    }

    // ── Reporte de Ventas Excel ───────────────────────────────────────────────

    public byte[] generarVentasExcel(LocalDate inicio, LocalDate fin, String estado) {
        LocalDateTime startDateTime = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime endDateTime = fin != null ? fin.atTime(23, 59, 59) : null;
        List<Reserva> reservas = reservaRepository.buscarReporte(startDateTime, endDateTime, estado);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte Ventas");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID Reserva", "Cliente", "Proyecto", "Fecha Reserva", "Estado", "Monto Anticipo (Bs.)", "Monto Total (Bs.)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Reserva r : reservas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getUsuario().getNombreCompleto());
                row.createCell(2).setCellValue(r.getCotizacion().getProyectoDiseno().getNombre());

                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(r.getFechaReserva());
                dateCell.setCellStyle(dateStyle);

                row.createCell(4).setCellValue(r.getEstado());
                row.createCell(5).setCellValue(r.getMontoAnticipo() != null ? r.getMontoAnticipo().doubleValue() : 0.0);
                row.createCell(6).setCellValue(r.getCotizacion().getTotal() != null ? r.getCotizacion().getTotal().doubleValue() : 0.0);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error al generar Excel de ventas", e);
            throw new RuntimeException("Error al generar Excel de ventas: " + e.getMessage(), e);
        }
    }

    // ── Reporte de Usuarios PDF ───────────────────────────────────────────────

    public byte[] generarUsuariosPdf(String rol, Boolean activo) {
        List<Usuario> usuarios = usuarioRepository.buscarReporte(rol, activo);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font infoFont = new Font(Font.HELVETICA, 10, Font.ITALIC, java.awt.Color.GRAY);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE);
            Font textFont = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.BLACK);

            Paragraph title = new Paragraph("Reporte de Usuarios Registrados", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph info = new Paragraph("Filtros: Rol: " + (rol != null ? rol : "TODOS") + " | Estado: " + (activo != null ? (activo ? "ACTIVO" : "INACTIVO") : "TODOS"), infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(20);
            document.add(info);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 25, 25, 18, 12, 12});

            java.awt.Color headerColor = new java.awt.Color(34, 139, 34);
            addCellWithBackground(table, "ID", headerFont, headerColor);
            addCellWithBackground(table, "Nombre Completo", headerFont, headerColor);
            addCellWithBackground(table, "Email", headerFont, headerColor);
            addCellWithBackground(table, "Usuario", headerFont, headerColor);
            addCellWithBackground(table, "Estado", headerFont, headerColor);
            addCellWithBackground(table, "Roles", headerFont, headerColor);

            for (Usuario u : usuarios) {
                table.addCell(new Phrase(u.getId().toString(), textFont));
                table.addCell(new Phrase(u.getNombreCompleto() != null ? u.getNombreCompleto() : "", textFont));
                table.addCell(new Phrase(u.getEmail(), textFont));
                table.addCell(new Phrase(u.getUsername(), textFont));
                table.addCell(new Phrase(u.getActivo() != null && u.getActivo() ? "ACTIVO" : "INACTIVO", textFont));

                StringBuilder userRoles = new StringBuilder();
                if (u.getRoles() != null) {
                    u.getRoles().forEach(r -> userRoles.append(r.getNombre()).append(" "));
                }
                table.addCell(new Phrase(userRoles.toString().trim(), textFont));
            }

            document.add(table);

            Paragraph totals = new Paragraph("\nTotal Usuarios Listados: " + usuarios.size(),
                    new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.DARK_GRAY));
            totals.setAlignment(Element.ALIGN_RIGHT);
            document.add(totals);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF de usuarios", e);
            throw new RuntimeException("Error al generar PDF de usuarios: " + e.getMessage(), e);
        }
    }

    // ── Reporte de Usuarios Excel ─────────────────────────────────────────────

    public byte[] generarUsuariosExcel(String rol, Boolean activo) {
        List<Usuario> usuarios = usuarioRepository.buscarReporte(rol, activo);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte Usuarios");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID Usuario", "Nombre Completo", "Email", "Nombre de Usuario", "Estado", "Roles"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Usuario u : usuarios) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getId());
                row.createCell(1).setCellValue(u.getNombreCompleto() != null ? u.getNombreCompleto() : "");
                row.createCell(2).setCellValue(u.getEmail());
                row.createCell(3).setCellValue(u.getUsername());
                row.createCell(4).setCellValue(u.getActivo() != null && u.getActivo() ? "Activo" : "Inactivo");

                StringBuilder userRoles = new StringBuilder();
                if (u.getRoles() != null) {
                    u.getRoles().forEach(r -> userRoles.append(r.getNombre()).append(" "));
                }
                row.createCell(5).setCellValue(userRoles.toString().trim());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error al generar Excel de usuarios", e);
            throw new RuntimeException("Error al generar Excel de usuarios: " + e.getMessage(), e);
        }
    }

    // ── Consultas para Previsualización JSON ──────────────────────────────────

    public List<Reserva> buscarReservas(LocalDateTime inicio, LocalDateTime fin, String estado) {
        return reservaRepository.buscarReporte(inicio, fin, estado);
    }

    public List<Usuario> buscarUsuarios(String rol, Boolean activo) {
        return usuarioRepository.buscarReporte(rol, activo);
    }

    // ── Helpers para Tablas de OpenPDF ────────────────────────────────────────

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addCellWithBackground(PdfPTable table, String text, Font font, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(bg);
        table.addCell(cell);
    }
}
