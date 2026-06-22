package com.decoraciones.domain.dtos.articuloinventario;

import java.time.LocalDateTime;

public record ImagenArticuloResponse(
        Long id,
        String url,
        Boolean esPrincipal,
        Integer orden,
        Boolean procesadoIa,
        LocalDateTime fechaSubida,
        String tipoVista
) {}
