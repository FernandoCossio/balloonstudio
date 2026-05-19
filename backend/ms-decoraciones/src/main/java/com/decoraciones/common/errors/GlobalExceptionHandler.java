package com.decoraciones.common.errors;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.common.response.ErrorResponse;
import com.decoraciones.common.response.FieldError;
import com.decoraciones.common.response.ValidationErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		BindingResult bindingResult = ex.getBindingResult();
		List<FieldError> errores = bindingResult.getFieldErrors().stream()
			.map(fieldError -> new FieldError(
				fieldError.getField(),
				ErrorCode.VALIDACION_CAMPO.getCode(),
				fieldError.getDefaultMessage()
			))
			.collect(Collectors.toList());

		ApiResponse<ValidationErrorResponse> body = new ApiResponse<>(
			"fail",
			new ValidationErrorResponse(errores),
			"Error de validación en la solicitud"
		);

		return ResponseEntity.status(ErrorCode.VALIDACION_CAMPO.getHttpStatus()).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleConstraintViolation(ConstraintViolationException ex) {
		List<FieldError> errores = ex.getConstraintViolations().stream()
			.map(this::toFieldError)
			.collect(Collectors.toList());

		ApiResponse<ValidationErrorResponse> body = new ApiResponse<>(
			"fail",
			new ValidationErrorResponse(errores),
			"Error de validación en la solicitud"
		);

		return ResponseEntity.status(ErrorCode.VALIDACION_CAMPO.getHttpStatus()).body(body);
	}

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleAppException(AppException ex) {
		return failWithCode(ex.getErrorCode());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
		return failWithCode(ErrorCode.ACCESO_DENEGADO);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnhandled(Exception ex) {
		String traceId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		log.error("Error no manejado [traceId={}]", traceId, ex);

		ErrorResponse body = new ErrorResponse(
			"error",
			ErrorCode.ERROR_INTERNO.getCode(),
			ErrorCode.ERROR_INTERNO.getDefaultMessage(),
			traceId
		);

		return ResponseEntity.status(ErrorCode.ERROR_INTERNO.getHttpStatus()).body(body);
	}

	private FieldError toFieldError(ConstraintViolation<?> violation) {
		String propertyPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
		String campo = extractLastPathSegment(propertyPath);
		return new FieldError(
			campo,
			ErrorCode.VALIDACION_CAMPO.getCode(),
			violation.getMessage()
		);
	}

	private static String extractLastPathSegment(String path) {
		int idx = path.lastIndexOf('.');
		if (idx >= 0 && idx + 1 < path.length()) {
			return path.substring(idx + 1);
		}
		return path;
	}

	private static ResponseEntity<ApiResponse<Map<String, Object>>> failWithCode(ErrorCode errorCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("code", errorCode.getCode());

		ApiResponse<Map<String, Object>> body = new ApiResponse<>(
			"fail",
			data,
			errorCode.getDefaultMessage()
		);

		return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
	}
}
