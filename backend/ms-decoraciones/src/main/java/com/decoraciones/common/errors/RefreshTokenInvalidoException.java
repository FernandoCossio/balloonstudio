package com.decoraciones.common.errors;

public class RefreshTokenInvalidoException extends AppException {
	public RefreshTokenInvalidoException() {
		super(ErrorCode.REFRESH_TOKEN_INVALIDO);
	}
}
