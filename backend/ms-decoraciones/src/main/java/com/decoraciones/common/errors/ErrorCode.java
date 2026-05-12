package com.decoraciones.common.errors;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	CREDENCIALES_INVALIDAS("AUT401", HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
	REFRESH_TOKEN_INVALIDO("AUT402", HttpStatus.UNAUTHORIZED, "Refresh token inválido o expirado"),
	REFRESH_TOKEN_ROBADO("AUT403", HttpStatus.UNAUTHORIZED, "Refresh token detectado como robado"),

	AUTH_TOKEN_NO_ENCONTRADO("AUTT101", HttpStatus.NOT_FOUND, "Token no encontrado"),
	AUTH_TOKEN_INVALIDO("AUTT201", HttpStatus.BAD_REQUEST, "Token inválido o expirado"),
	PASSWORD_NO_COINCIDE("AUTT202", HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden"),
	CUENTA_YA_ACTIVA("AUTT203", HttpStatus.CONFLICT, "La cuenta ya está activa"),

	USUARIO_NO_ENCONTRADO("USR101", HttpStatus.NOT_FOUND, "Usuario no encontrado"),
	USUARIO_DUPLICADO("USR201", HttpStatus.CONFLICT, "Username o email ya existe"),

	ROL_NO_ENCONTRADO("ROL101", HttpStatus.NOT_FOUND, "Rol no encontrado"),

	CATEGORIA_NO_ENCONTRADA("CAT101", HttpStatus.NOT_FOUND, "Categoría no encontrada"),
	CATEGORIA_DUPLICADA("CAT201", HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre"),

	EMPLEADO_NO_ENCONTRADO("EMP101", HttpStatus.NOT_FOUND, "Empleado no encontrado"),
	EMPLEADO_CI_DUPLICADO("EMP201", HttpStatus.CONFLICT, "Ya existe un empleado con ese CI"),
	EMPLEADO_EMAIL_DUPLICADO("EMP202", HttpStatus.CONFLICT, "Ya existe un empleado con ese email"),

	ARTICULO_INVENTARIO_NO_ENCONTRADO("INV101", HttpStatus.NOT_FOUND, "Artículo de inventario no encontrado"),

	VALIDACION_CAMPO("VAL301", HttpStatus.BAD_REQUEST, "Error de validación en la solicitud"),
	EMAIL_OBLIGATORIO("VAL302", HttpStatus.BAD_REQUEST, "El email es obligatorio"),

	NO_AUTENTICADO("SEC401", HttpStatus.UNAUTHORIZED, "No autenticado"),
	ACCESO_DENEGADO("SEC501", HttpStatus.FORBIDDEN, "Acceso denegado"),

	ERROR_INTERNO("SYS901", HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado.");

	private final String code;
	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}
}
