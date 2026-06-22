package com.decoraciones.common.errors;

import org.springframework.http.HttpStatus;

/**
 * Catálogo centralizado de errores de negocio.
 *
 * Convención de códigos: [PREFIJO][CATEGORÍA][SECUENCIA]
 *
 * PREFIJOS por dominio:
 *   AUT → Autenticación de sesión (login, refresh, credenciales)
 *   TKN → Tokens de verificación de cuenta (activación, reset de password)
 *   USR → Usuarios
 *   ROL → Roles
 *   CAT → Categorías
 *   EMP → Empleados
 *   INV → Inventario
 *   VAL → Validación de campos
 *   SEC → Seguridad (autenticación/autorización a nivel de acceso)
 *   SYS → Sistema / errores inesperados
 *
 * CATEGORÍA numérica:
 *   1xx → Not Found
 *   2xx → Conflict / estado inválido (duplicados, reglas de negocio)
 *   3xx → Validación de entrada
 *   4xx → No autenticado / credenciales inválidas
 *   5xx → Acceso denegado
 *   9xx → Error interno del servidor
 */
public enum ErrorCode {

    // -------------------------------------------------------------------------
    // AUT — Autenticación de sesión
    // -------------------------------------------------------------------------
    CREDENCIALES_INVALIDAS("AUT401", HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
    REFRESH_TOKEN_INVALIDO("AUT402", HttpStatus.UNAUTHORIZED, "Refresh token inválido o expirado"),
    REFRESH_TOKEN_ROBADO("AUT403", HttpStatus.UNAUTHORIZED, "Refresh token detectado como robado"),

    // -------------------------------------------------------------------------
    // TKN — Tokens de verificación de cuenta (activación, reset de password)
    // -------------------------------------------------------------------------
    TOKEN_NO_ENCONTRADO("TKN101", HttpStatus.NOT_FOUND, "Token no encontrado"),
    TOKEN_INVALIDO("TKN201", HttpStatus.BAD_REQUEST, "Token inválido o expirado"),
    PASSWORD_NO_COINCIDE("TKN202", HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden"),
    CUENTA_YA_ACTIVA("TKN203", HttpStatus.CONFLICT, "La cuenta ya está activa"),

    // -------------------------------------------------------------------------
    // USR — Usuarios
    // -------------------------------------------------------------------------
    USUARIO_NO_ENCONTRADO("USR101", HttpStatus.NOT_FOUND, "Usuario no encontrado"),
    USUARIO_DUPLICADO("USR201", HttpStatus.CONFLICT, "Username o email ya existe"),

    // -------------------------------------------------------------------------
    // ROL — Roles
    // -------------------------------------------------------------------------
    ROL_NO_ENCONTRADO("ROL101", HttpStatus.NOT_FOUND, "Rol no encontrado"),

    // -------------------------------------------------------------------------
    // CAT — Categorías
    // -------------------------------------------------------------------------
    CATEGORIA_NO_ENCONTRADA("CAT101", HttpStatus.NOT_FOUND, "Categoría no encontrada"),
    CATEGORIA_DUPLICADA("CAT201", HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre"),

    // -------------------------------------------------------------------------
    // EMP — Empleados
    // -------------------------------------------------------------------------
    EMPLEADO_NO_ENCONTRADO("EMP101", HttpStatus.NOT_FOUND, "Empleado no encontrado"),
    EMPLEADO_CI_DUPLICADO("EMP201", HttpStatus.CONFLICT, "Ya existe un empleado con ese CI"),
    EMPLEADO_EMAIL_DUPLICADO("EMP202", HttpStatus.CONFLICT, "Ya existe un empleado con ese email"),
    AUTO_DESACTIVACION_PROHIBIDA("EMP203", HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta"),

    // -------------------------------------------------------------------------
    // INV — Inventario
    // -------------------------------------------------------------------------
    ARTICULO_INVENTARIO_NO_ENCONTRADO("INV101", HttpStatus.NOT_FOUND, "Artículo de inventario no encontrado"),
    IMAGEN_NO_ENCONTRADA("INV102", HttpStatus.NOT_FOUND, "Imagen no encontrada"),
    IMAGEN_NO_PERTENECE_AL_ARTICULO("INV202", HttpStatus.BAD_REQUEST, "La imagen no pertenece al artículo especificado"),
    STOCK_INSUFICIENTE("INV203", HttpStatus.CONFLICT, "Stock insuficiente para el artículo"),

    // -------------------------------------------------------------------------
    // RES — Reservas
    // -------------------------------------------------------------------------
    RESERVA_NO_ENCONTRADA("RES101", HttpStatus.NOT_FOUND, "Reserva no encontrada"),
    ESTADO_RESERVA_INVALIDO("RES201", HttpStatus.BAD_REQUEST, "Estado de reserva inválido"),

    // -------------------------------------------------------------------------
    // INC — Incidencias
    // -------------------------------------------------------------------------
    INCIDENCIA_NO_ENCONTRADA("INC101", HttpStatus.NOT_FOUND, "Incidencia no encontrada"),

    // -------------------------------------------------------------------------
    // VAL — Validación de campos
    // Nota: errores de campo individuales los maneja GlobalExceptionHandler
    // automáticamente vía @Valid. Solo se registra aquí el código base.
    // -------------------------------------------------------------------------
    VALIDACION_CAMPO("VAL301", HttpStatus.BAD_REQUEST, "Error de validación en la solicitud"),

    // -------------------------------------------------------------------------
    // SEC — Seguridad
    // -------------------------------------------------------------------------
    NO_AUTENTICADO("SEC401", HttpStatus.UNAUTHORIZED, "No autenticado"),
    ACCESO_DENEGADO("SEC501", HttpStatus.FORBIDDEN, "Acceso denegado"),

    // -------------------------------------------------------------------------
    // SYS — Sistema
    // -------------------------------------------------------------------------
    ERROR_INTERNO("SYS901", HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getDefaultMessage() { return defaultMessage; }
}