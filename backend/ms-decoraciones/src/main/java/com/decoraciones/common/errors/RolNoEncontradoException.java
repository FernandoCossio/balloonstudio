package com.decoraciones.common.errors;

public class RolNoEncontradoException extends AppException {
	public RolNoEncontradoException() {
		super(ErrorCode.ROL_NO_ENCONTRADO);
	}
}
