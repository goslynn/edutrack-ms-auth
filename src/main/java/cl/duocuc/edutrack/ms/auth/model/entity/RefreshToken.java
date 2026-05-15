package cl.duocuc.edutrack.ms.auth.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", schema = "auth")
public class RefreshToken extends CreatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    public String tokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean revoked = false;

    @Column(name = "revoked_at")
    public Instant revokedAt;
}
