package com.decoraciones.common.errors;

public class AuthTokenNoEncontradoException extends AppException {
	public AuthTokenNoEncontradoException() {
		super(ErrorCode.AUTH_TOKEN_NO_ENCONTRADO);
	}
}
