package com.decoraciones.features.auth;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.auth.LoginRequest;
import com.decoraciones.domain.dtos.auth.TokenResponse;
import com.decoraciones.domain.dtos.usuario.RegistrarClienteDto;
import com.decoraciones.domain.dtos.usuario.ResponseUsuarioDto;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/auth/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse response
	) {
		AuthService.TokenWithCookie result = authService.login(request);
		response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
		return ResponseEntity.ok(ApiResponse.success(result.tokenResponse()));
	}

	@PostMapping("/auth/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
		@CookieValue(name = "refresh_token", required = false) String rawToken,
		HttpServletResponse response
	) {
		AuthService.TokenWithCookie result = authService.refresh(rawToken);
		response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
		return ResponseEntity.ok(ApiResponse.success(result.tokenResponse()));
	}

	@PostMapping("/auth/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@AuthenticationPrincipal Jwt jwt,
		@CookieValue(name = "refresh_token", required = false) String rawToken,
		HttpServletResponse response
	) {
		if (jwt == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		ResponseCookie clearCookie = authService.logout(rawToken);
		response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null));
	}

	@PostMapping("/auth/register")
	public ResponseEntity<ApiResponse<ResponseUsuarioDto>> register(@Valid @RequestBody RegistrarClienteDto request) {
		ResponseUsuarioDto created = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(created, "Cliente registrado correctamente. Revisa tu email para activar tu cuenta"));
	}
}
