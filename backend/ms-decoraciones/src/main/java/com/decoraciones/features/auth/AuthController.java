package com.decoraciones.features.auth;

import com.decoraciones.auth.jwt.JwtService;
import com.decoraciones.auth.userdetails.UsuarioPrincipal;
import com.decoraciones.common.response.ApiResponse;
import com.decoraciones.common.errors.ErrorCode;
import com.decoraciones.common.errors.RefreshTokenException;
import com.decoraciones.domain.dtos.auth.LoginRequest;
import com.decoraciones.domain.dtos.auth.TokenResponse;
import com.decoraciones.domain.dtos.usuario.RegistrarClienteDto;
import com.decoraciones.domain.dtos.usuario.ResponseUsuarioDto;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.refreshtoken.RefreshTokenService;
import com.decoraciones.features.usuario.UsuarioService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UsuarioService usuarioService;
	private final RefreshTokenService refreshTokenService;

	public AuthController(
		AuthenticationManager authenticationManager,
		JwtService jwtService,
		UsuarioService usuarioService,
		RefreshTokenService refreshTokenService
	) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.usuarioService = usuarioService;
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/auth/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse response
	) {
		try {
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password())
			);

			UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
			String accessToken = jwtService.generateAccessToken(authentication);
			String rawToken = refreshTokenService.create(principal.getId());

			ResponseCookie cookie = buildRefreshCookie(rawToken, Duration.ofDays(7).getSeconds());
			response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

			return ResponseEntity.ok(ApiResponse.success(new TokenResponse(accessToken)));
		} catch (AuthenticationException ex) {
			throw ex;
		}
	}

	@PostMapping("/auth/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
		@CookieValue(name = "refresh_token", required = false) String rawToken,
		HttpServletResponse response
	) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new RefreshTokenException(ErrorCode.REFRESH_TOKEN_INVALIDO);
		}

		RefreshTokenService.RotationResult rotation = refreshTokenService.findAndRotate(rawToken);
		ResponseCookie cookie = buildRefreshCookie(rotation.rawToken(), Duration.ofDays(7).getSeconds());
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		Usuario usuario = rotation.usuario();
		UsuarioPrincipal principal = UsuarioPrincipal.from(usuario);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			principal.getAuthorities()
		);

		String accessToken = jwtService.generateAccessToken(authentication);
		return ResponseEntity.ok(ApiResponse.success(new TokenResponse(accessToken)));
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

		if (rawToken != null && !rawToken.isBlank()) {
			refreshTokenService.revokeByRawToken(rawToken);
		}

		response.addHeader(HttpHeaders.SET_COOKIE, buildClearRefreshCookie().toString());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null));
	}

	@PostMapping("/auth/register")
	public ResponseEntity<ApiResponse<ResponseUsuarioDto>> register(@Valid @RequestBody RegistrarClienteDto request) {
		ResponseUsuarioDto response = usuarioService.registrarCliente(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	private static ResponseCookie buildRefreshCookie(String rawToken, long maxAgeSeconds) {
		return ResponseCookie.from("refresh_token", rawToken)
			.httpOnly(true)
			.secure(true)
			.sameSite("Strict")
			.path("/auth/refresh")
			.maxAge(maxAgeSeconds)
			.build();
	}

	private static ResponseCookie buildClearRefreshCookie() {
		return buildRefreshCookie("", 0);
	}
}
