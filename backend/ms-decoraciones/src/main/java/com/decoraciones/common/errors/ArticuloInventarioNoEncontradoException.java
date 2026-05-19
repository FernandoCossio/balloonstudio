package com.decoraciones.common.errors;

public class ArticuloInventarioNoEncontradoException extends AppException {
	public ArticuloInventarioNoEncontradoException() {
		super(ErrorCode.ARTICULO_INVENTARIO_NO_ENCONTRADO);
	}
}
