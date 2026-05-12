package com.decoraciones.common.errors;

public class AuthTokenInvalidoException extends AppException {
	public AuthTokenInvalidoException() {
		super(ErrorCode.TOKEN_INVALIDO);
	}
}
