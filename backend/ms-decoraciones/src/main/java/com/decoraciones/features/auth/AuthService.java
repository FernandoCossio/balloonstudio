package com.decoraciones.features.auth;

import com.decoraciones.auth.jwt.JwtService;
import com.decoraciones.auth.userdetails.UsuarioPrincipal;
import com.decoraciones.common.errors.RefreshTokenInvalidoException;
import com.decoraciones.domain.dtos.auth.LoginRequest;
import com.decoraciones.domain.dtos.auth.TokenResponse;
import com.decoraciones.domain.dtos.usuario.RegistrarClienteDto;
import com.decoraciones.domain.dtos.usuario.ResponseUsuarioDto;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.refreshtoken.RefreshTokenService;
import com.decoraciones.features.usuario.UsuarioService;

import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Arrays;

@Service
@Transactional
public class AuthService {

	private static final long REFRESH_MAX_AGE_SECONDS = Duration.ofDays(7).getSeconds();

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UsuarioService usuarioService;
	private final RefreshTokenService refreshTokenService;
	private final boolean refreshCookieSecure;
	private final String refreshCookiePath;

	public AuthService(
		AuthenticationManager authenticationManager,
		JwtService jwtService,
		UsuarioService usuarioService,
		RefreshTokenService refreshTokenService,
		Environment environment,
		@Value("${server.servlet.context-path:}") String contextPath
	) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.usuarioService = usuarioService;
		this.refreshTokenService = refreshTokenService;

		boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
		this.refreshCookieSecure = !isDev;

		String normalizedContextPath = contextPath == null ? "" : contextPath.trim();
		if (!normalizedContextPath.isEmpty() && !normalizedContextPath.startsWith("/")) {
			normalizedContextPath = "/" + normalizedContextPath;
		}
		this.refreshCookiePath = (normalizedContextPath.isEmpty() ? "" : normalizedContextPath) + "/auth";
	}

	public TokenWithCookie login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);

		UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
		String accessToken = jwtService.generateAccessToken(authentication);
		String rawToken = refreshTokenService.create(principal.getId());

		ResponseCookie cookie = buildRefreshCookie(rawToken, REFRESH_MAX_AGE_SECONDS);
		return new TokenWithCookie(cookie, new TokenResponse(accessToken));
	}

	public TokenWithCookie refresh(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new RefreshTokenInvalidoException();
		}

		RefreshTokenService.RotationResult rotation = refreshTokenService.findAndRotate(rawToken);
		ResponseCookie cookie = buildRefreshCookie(rotation.rawToken(), REFRESH_MAX_AGE_SECONDS);

		Usuario usuario = rotation.usuario();
		UsuarioPrincipal principal = UsuarioPrincipal.from(usuario);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			principal.getAuthorities()
		);

		String accessToken = jwtService.generateAccessToken(authentication);
		return new TokenWithCookie(cookie, new TokenResponse(accessToken));
	}

	public ResponseCookie logout(String rawToken) {
		if (rawToken != null && !rawToken.isBlank()) {
			refreshTokenService.revokeByRawToken(rawToken);
		}

		return buildClearRefreshCookie();
	}

	public ResponseUsuarioDto register(RegistrarClienteDto request) {
		return usuarioService.registrarCliente(request);
	}

	private ResponseCookie buildRefreshCookie(String rawToken, long maxAgeSeconds) {
		return ResponseCookie.from("refresh_token", rawToken)
			.httpOnly(true)
			.secure(refreshCookieSecure)
			.sameSite("Strict")
			.path(refreshCookiePath)
			.maxAge(maxAgeSeconds)
			.build();
	}

	private ResponseCookie buildClearRefreshCookie() {
		return buildRefreshCookie("", 0);
	}

	public record TokenWithCookie(ResponseCookie refreshCookie, TokenResponse tokenResponse) {
	}
}
