package com.decoraciones.common.errors;

public class EmpleadoCiDuplicadoException extends AppException {
	public EmpleadoCiDuplicadoException() {
		super(ErrorCode.EMPLEADO_CI_DUPLICADO);
	}
}
