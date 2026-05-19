package com.decoraciones.common.errors;

public class CuentaYaActivaException extends AppException {
	public CuentaYaActivaException() {
		super(ErrorCode.CUENTA_YA_ACTIVA);
	}
}
