package com.decoraciones.features.refreshtoken;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCleanupJob {

	private final RefreshTokenRepository refreshTokenRepository;

	public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Scheduled(cron = "0 0 2 * * *")
	@Transactional
	public void cleanupExpiredTokens() {
		refreshTokenRepository.deleteExpiredGlobal(Instant.now());
	}
}
