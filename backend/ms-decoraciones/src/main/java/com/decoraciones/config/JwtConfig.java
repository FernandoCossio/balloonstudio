package com.decoraciones.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Period;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

	@Bean
	KeyPair jwtKeyPair() {
		return generateRsaKey();
	}

	@Bean
	JwtDecoder jwtDecoder(@Value("${app.jwt.public-key}") RSAPublicKey publicKey) {
		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}

	@Bean
	JwtEncoder jwtEncoder(
		@Value("${app.jwt.public-key}") RSAPublicKey publicKey,
		@Value("${app.jwt.private-key}") RSAPrivateKey privateKey
	) {
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
			.privateKey(privateKey)
			.keyID(UUID.randomUUID().toString())
			.build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
	}

	@Bean
	JwtSettings jwtSettings(
		@Value("${app.jwt.issuer}") String issuer,
		@Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl,
		@Value("${app.jwt.refresh-token-ttl}") String refreshTokenTtl
	) {
		return new JwtSettings(issuer, accessTokenTtl, parseRefreshTokenTtl(refreshTokenTtl));
	}

	private static Duration parseRefreshTokenTtl(String value) {
		try {
			return Duration.parse(value);
		} catch (Exception ignored) {
		}

		Period period;
		try {
			period = Period.parse(value);
		} catch (Exception e) {
			throw new IllegalStateException("Formato inválido para app.jwt.refresh-token-ttl: " + value, e);
		}

		if (period.getYears() != 0 || period.getMonths() != 0) {
			throw new IllegalStateException("Solo se soportan periodos en días para app.jwt.refresh-token-ttl: " + value);
		}

		return Duration.ofDays(period.getDays());
	}

	private static KeyPair generateRsaKey() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo generar el par de llaves RSA", e);
		}
	}

	public record JwtSettings(String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
	}
}
