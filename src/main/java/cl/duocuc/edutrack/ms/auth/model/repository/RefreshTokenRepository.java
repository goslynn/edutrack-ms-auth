package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, UUID> {

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    /** Active (non-revoked, non-expired) tokens for a user. */
    public List<RefreshToken> findActiveByUserId(UUID userId) {
        return list("user.id = ?1 and revoked = false and expiresAt > ?2",
            userId, Instant.now());
    }

    /**
     * Revokes all active tokens for a user.
     * Called on logout, password change, or forced session invalidation (BE-AUTH-003).
     */
    public long revokeAllByUserId(UUID userId) {
        return update("revoked = true, revokedAt = ?1 where user.id = ?2 and revoked = false",
            Instant.now(), userId);
    }

    /** Housekeeping: removes tokens that expired before the given cutoff. */
    public long deleteExpiredBefore(Instant cutoff) {
        return delete("expiresAt < ?1", cutoff);
    }
}
