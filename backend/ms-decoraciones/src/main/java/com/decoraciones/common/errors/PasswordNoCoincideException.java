package com.decoraciones.common.errors;

public class PasswordNoCoincideException extends AppException {
	public PasswordNoCoincideException() {
		super(ErrorCode.PASSWORD_NO_COINCIDE);
	}
}
