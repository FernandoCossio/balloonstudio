package com.decoraciones.common.errors;

public class RefreshTokenRobadoException extends AppException {
	public RefreshTokenRobadoException() {
		super(ErrorCode.REFRESH_TOKEN_ROBADO);
	}
}
