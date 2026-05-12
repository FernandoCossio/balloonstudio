package com.decoraciones.common.errors;

public class CategoriaNoEncontradaException extends AppException {
	public CategoriaNoEncontradaException() {
		super(ErrorCode.CATEGORIA_NO_ENCONTRADA);
	}
}
