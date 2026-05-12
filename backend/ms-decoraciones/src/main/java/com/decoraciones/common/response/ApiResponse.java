package com.decoraciones.common.response;

public record ApiResponse<T>(String status, T data, String message) {
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>("success", data, null);
	}
}
