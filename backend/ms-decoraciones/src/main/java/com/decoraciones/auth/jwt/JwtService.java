package com.decoraciones.auth.jwt;

import com.decoraciones.auth.userdetails.UsuarioPrincipal;
import com.decoraciones.config.JwtConfig.JwtSettings;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtSettings jwtSettings;

	public JwtService(JwtEncoder jwtEncoder, JwtSettings jwtSettings) {
		this.jwtEncoder = jwtEncoder;
		this.jwtSettings = jwtSettings;
	}

	public String generateAccessToken(Authentication authentication) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(jwtSettings.accessTokenTtl());

		List<String> roles = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.map(JwtService::mapToAppRole)
			.filter(role -> role != null && !role.isBlank())
			.distinct()
			.map(String::trim)
			.collect(Collectors.toList());

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(jwtSettings.issuer())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.subject(authentication.getName())
			.claim("roles", roles);

		Object principal = authentication.getPrincipal();
		if (principal instanceof UsuarioPrincipal usuarioPrincipal) {
			claims.claim("uid", usuarioPrincipal.getId());
		}

		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()))
			.getTokenValue();
	}

	private static String mapToAppRole(String authority) {
		if (authority == null) return null;

		String value = authority.trim();
		if (value.isEmpty()) return null;

		if (value.startsWith("role_")) {
			return value.toLowerCase(Locale.ROOT);
		}

		String upper = value.toUpperCase(Locale.ROOT);
		return switch (upper) {
			case "ROLE_ADMIN" -> "role_administrador";
			case "ROLE_CLIENTE" -> "role_cliente";
			case "ROLE_EMPLEADO" -> "role_empleado";
			default -> null;
		};
	}
}
