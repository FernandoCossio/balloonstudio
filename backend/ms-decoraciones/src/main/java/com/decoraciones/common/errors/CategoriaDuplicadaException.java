package com.decoraciones.common.errors;

public class CategoriaDuplicadaException extends AppException {
	public CategoriaDuplicadaException() {
		super(ErrorCode.CATEGORIA_DUPLICADA);
	}
}
