package com.decoraciones.features.refreshtoken;

import com.decoraciones.domain.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @EntityGraph(attributePaths = {"usuario", "usuario.roles"})
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.family = :family")
    void revokeAllByFamily(String family);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.usuario.id = :usuarioId")
    void revokeAllByUsuarioId(Long usuarioId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.usuario.id = :usuarioId AND r.expiresAt < :now")
    void deleteExpiredByUsuarioId(Long usuarioId, Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteExpiredGlobal(Instant now);
}
