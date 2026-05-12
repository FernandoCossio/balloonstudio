package com.decoraciones.domain.models;

import com.decoraciones.domain.enums.auth.TipoToken;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class AuthToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoToken tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    /**
     * Verifica si el token ha expirado.
     * @return true si la fecha actual es posterior a la fecha de expiración.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Verifica si el token es válido para su uso.
     * Un token es válido si no ha sido revocado, no ha sido usado y no ha expirado.
     * @return true si el token es válido.
     */
    public boolean isValid() {
        return !isRevoked && usedAt == null && !isExpired();
    }
}
