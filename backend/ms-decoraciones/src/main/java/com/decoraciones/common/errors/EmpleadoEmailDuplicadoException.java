package com.decoraciones.common.errors;

public class EmpleadoEmailDuplicadoException extends AppException {
	public EmpleadoEmailDuplicadoException() {
		super(ErrorCode.EMPLEADO_EMAIL_DUPLICADO);
	}
}
