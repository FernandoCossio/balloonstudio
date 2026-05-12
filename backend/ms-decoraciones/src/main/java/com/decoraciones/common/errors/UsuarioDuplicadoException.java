package com.decoraciones.common.errors;

public class UsuarioDuplicadoException extends AppException {
	public UsuarioDuplicadoException() {
		super(ErrorCode.USUARIO_DUPLICADO);
	}
}
