package com.decoraciones.common.errors;

public class RefreshTokenException extends RuntimeException {
	private final ErrorCode errorCode;

	public RefreshTokenException(ErrorCode errorCode) {
		super(errorCode.getDefaultMessage());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
