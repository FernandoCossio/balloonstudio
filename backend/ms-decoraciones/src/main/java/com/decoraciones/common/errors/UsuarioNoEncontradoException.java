package com.decoraciones.common.errors;

public class UsuarioNoEncontradoException extends AppException {
	public UsuarioNoEncontradoException() {
		super(ErrorCode.USUARIO_NO_ENCONTRADO);
	}
}
