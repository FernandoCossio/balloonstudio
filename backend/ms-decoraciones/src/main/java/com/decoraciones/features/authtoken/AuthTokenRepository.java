package com.decoraciones.features.authtoken;

import com.decoraciones.domain.enums.auth.TipoToken;
import com.decoraciones.domain.models.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByToken(String token);

    Optional<AuthToken> findByTokenAndTipo(String token, TipoToken tipo);

    @Modifying
    @Query("UPDATE AuthToken t SET t.isRevoked = true WHERE t.usuario.id = :usuarioId AND t.tipo = :tipo")
    void revokeAllByUsuarioIdAndTipo(Long usuarioId, TipoToken tipo);

    @Modifying
    @Query("UPDATE AuthToken t SET t.isRevoked = true WHERE t.usuario.id = :usuarioId")
    void revokeAllByUsuarioId(Long usuarioId);
}
