package com.decoraciones.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ValidationErrorResponse(@JsonProperty("errores_validacion") List<FieldError> erroresValidacion) {
}
