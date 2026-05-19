package com.decoraciones.features.auth;

import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.domain.dtos.auth.LoginRequest;
import com.decoraciones.domain.dtos.auth.TokenResponse;
import com.decoraciones.domain.dtos.usuario.RegistrarClienteDto;
import com.decoraciones.domain.dtos.usuario.ResponseUsuarioDto;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse response
	) {
		AuthService.TokenWithCookie result = authService.login(request);
		response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
		response.addHeader(HttpHeaders.SET_COOKIE, result.accessCookie().toString());
		return ResponseEntity.ok(ApiResponse.success(result.tokenResponse()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
		@CookieValue(name = "refresh_token", required = false) String rawToken,
		HttpServletResponse response
	) {
		AuthService.TokenWithCookie result = authService.refresh(rawToken);
		response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
		response.addHeader(HttpHeaders.SET_COOKIE, result.accessCookie().toString());
		return ResponseEntity.ok(ApiResponse.success(result.tokenResponse()));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@CookieValue(name = "refresh_token", required = false) String rawToken,
		HttpServletResponse response
	) {
		ResponseCookie[] cookies = authService.logout(rawToken);
		for (ResponseCookie c : cookies) {
			response.addHeader(HttpHeaders.SET_COOKIE, c.toString());
		}
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null));
	}

	@GetMapping("/me/token")
	public ResponseEntity<ApiResponse<String>> getCurrentToken(@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(ApiResponse.success(jwt.getTokenValue()));
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<ResponseUsuarioDto>> register(@Valid @RequestBody RegistrarClienteDto request) {
		ResponseUsuarioDto created = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(created, "Cliente registrado correctamente. Revisa tu email para activar tu cuenta"));
	}
}
