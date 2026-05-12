package com.decoraciones.common.errors;

public class EmpleadoNoEncontradoException extends AppException {
	public EmpleadoNoEncontradoException() {
		super(ErrorCode.EMPLEADO_NO_ENCONTRADO);
	}
}
