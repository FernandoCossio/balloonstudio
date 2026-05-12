package com.decoraciones.common.errors;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	CREDENCIALES_INVALIDAS("AUT401", HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
	REFRESH_TOKEN_INVALIDO("AUT402", HttpStatus.UNAUTHORIZED, "Refresh token inválido o expirado"),
	REFRESH_TOKEN_ROBADO("AUT403", HttpStatus.UNAUTHORIZED, "Refresh token detectado como robado"),

	USUARIO_NO_ENCONTRADO("USR101", HttpStatus.NOT_FOUND, "Usuario no encontrado"),
	USUARIO_DUPLICADO("USR201", HttpStatus.CONFLICT, "Username o email ya existe"),

	VALIDACION_CAMPO("VAL301", HttpStatus.BAD_REQUEST, "Error de validación en la solicitud"),

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
