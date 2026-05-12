package com.decoraciones.features.refreshtoken;

import com.decoraciones.common.errors.ErrorCode;
import com.decoraciones.common.errors.RefreshTokenException;
import com.decoraciones.config.JwtConfig.JwtSettings;
import com.decoraciones.domain.models.RefreshToken;
import com.decoraciones.domain.models.Usuario;
import com.decoraciones.features.usuario.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtSettings jwtSettings;
	private final UsuarioRepository usuarioRepository;

	public RefreshTokenService(
		RefreshTokenRepository refreshTokenRepository,
		JwtSettings jwtSettings,
		UsuarioRepository usuarioRepository
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtSettings = jwtSettings;
		this.usuarioRepository = usuarioRepository;
	}

	public String create(Long usuarioId) {
		Instant now = Instant.now();
		refreshTokenRepository.deleteExpiredByUsuarioId(usuarioId, now);

		Usuario usuarioRef = usuarioRepository.getReferenceById(usuarioId);
		String rawToken = UUID.randomUUID().toString();
		String hashedToken = DigestUtils.sha256Hex(rawToken);

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUsuario(usuarioRef);
		refreshToken.setToken(hashedToken);
		refreshToken.setFamily(UUID.randomUUID().toString());
		refreshToken.setExpiresAt(now.plus(jwtSettings.refreshTokenTtl()));
		refreshToken.setRevoked(false);

		refreshTokenRepository.save(refreshToken);
		return rawToken;
	}

	public record RotationResult(String rawToken, Usuario usuario) {
	}

	public RotationResult findAndRotate(String rawToken) {
		String hashedToken = DigestUtils.sha256Hex(rawToken);
		RefreshToken stored = refreshTokenRepository.findByToken(hashedToken)
			.orElseThrow(() -> new RefreshTokenException(ErrorCode.REFRESH_TOKEN_INVALIDO));
		String newRawToken = rotate(stored);
		return new RotationResult(newRawToken, stored.getUsuario());
	}

	@Transactional(noRollbackFor = RefreshTokenException.class)
	public String rotate(RefreshToken current) {
		if (current.isRevoked()) {
			refreshTokenRepository.revokeAllByFamily(current.getFamily());
			refreshTokenRepository.flush();
			throw new RefreshTokenException(ErrorCode.REFRESH_TOKEN_ROBADO);
		}

		if (current.isExpired()) {
			throw new RefreshTokenException(ErrorCode.REFRESH_TOKEN_INVALIDO);
		}

		current.setRevoked(true);
		refreshTokenRepository.save(current);

		Instant now = Instant.now();
		String newRawToken = UUID.randomUUID().toString();
		String newHashedToken = DigestUtils.sha256Hex(newRawToken);

		RefreshToken next = new RefreshToken();
		next.setUsuario(current.getUsuario());
		next.setToken(newHashedToken);
		next.setFamily(current.getFamily());
		next.setExpiresAt(now.plus(jwtSettings.refreshTokenTtl()));
		next.setRevoked(false);

		refreshTokenRepository.save(next);
		return newRawToken;
	}

	public void revokeByRawToken(String rawToken) {
		String hashedToken = DigestUtils.sha256Hex(rawToken);
		refreshTokenRepository.findByToken(hashedToken).ifPresent(refreshToken -> {
			if (!refreshToken.isRevoked()) {
				refreshToken.setRevoked(true);
				refreshTokenRepository.save(refreshToken);
			}
		});
	}

	public void revokeAllByUsuario(Long usuarioId) {
		refreshTokenRepository.revokeAllByUsuarioId(usuarioId);
	}
}
