package com.decoraciones.common.errors;

public class AuthTokenNoEncontradoException extends AppException {
	public AuthTokenNoEncontradoException() {
		super(ErrorCode.TOKEN_NO_ENCONTRADO);
	}
}
