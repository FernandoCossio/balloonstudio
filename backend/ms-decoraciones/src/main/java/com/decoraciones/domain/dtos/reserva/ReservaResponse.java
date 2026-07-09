package com.decoraciones.domain.dtos.reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResponse(
    Long id,
    Long usuarioId,
    String nombreCliente,
    String emailCliente,
    String telefonoCliente,
    Long proyectoId,
    String nombreProyecto,
    LocalDate fechaEvento,
    String lugarEvento,
    Long cotizacionId,
    BigDecimal costoArticulos,
    BigDecimal costoFlete,
    BigDecimal costoArmado,
    BigDecimal total,
    BigDecimal montoAnticipo,
    String estado,
    LocalDateTime fechaReserva,
    LocalDateTime fechaLimitePago,
    LocalDateTime fechaConfirmacion,
    Long empleadoAsignadoId
) {}
