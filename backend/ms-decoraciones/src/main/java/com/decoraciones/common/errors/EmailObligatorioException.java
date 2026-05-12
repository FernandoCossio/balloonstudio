package com.decoraciones.common.errors;

public class EmailObligatorioException extends AppException {
	public EmailObligatorioException() {
		super(ErrorCode.EMAIL_OBLIGATORIO);
	}
}
