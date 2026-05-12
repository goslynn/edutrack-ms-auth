package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.RefreshToken;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class RefreshTokenRepositoryTest {

    @Inject
    RefreshTokenRepository repo;

    private User buildUser(String email) {
        User u = new User();
        u.displayName = "Test User";
        u.email = email;
        u.passwordHash = "$2a$10$placeholder";
        u.persist();
        return u;
    }

    private RefreshToken buildToken(User user, String hash, boolean revoked, Instant expiresAt) {
        RefreshToken t = new RefreshToken();
        t.user = user;
        t.tokenHash = hash;
        t.revoked = revoked;
        t.expiresAt = expiresAt;
        if (revoked) t.revokedAt = Instant.now();
        t.persist();
        return t;
    }

    // ── findByTokenHash ───────────────────────────────────────────────────────

    @Test
    void findByTokenHash_retornaToken() {
        User u = buildUser("rt_repo_find@test.com");
        String hash = "hash_repo_" + UUID.randomUUID();
        buildToken(u, hash, false, Instant.now().plusSeconds(3600));
        RefreshToken.getEntityManager().flush();

        Optional<RefreshToken> found = repo.findByTokenHash(hash);

        assertTrue(found.isPresent());
        assertEquals(hash, found.get().tokenHash);
    }

    @Test
    void findByTokenHash_retornaVacioParaHashInexistente() {
        assertTrue(repo.findByTokenHash("hash_inexistente").isEmpty());
    }

    // ── findActiveByUserId ────────────────────────────────────────────────────

    @Test
    void findActiveByUserId_excluyeRevocadosYExpirados() {
        User u = buildUser("rt_repo_active@test.com");
        buildToken(u, "hash_active_"   + UUID.randomUUID(), false, Instant.now().plusSeconds(3600));
        buildToken(u, "hash_revoked_"  + UUID.randomUUID(), true,  Instant.now().plusSeconds(3600));
        buildToken(u, "hash_expired_"  + UUID.randomUUID(), false, Instant.now().minusSeconds(60));
        RefreshToken.getEntityManager().flush();

        List<RefreshToken> activos = repo.findActiveByUserId(u.id);

        assertEquals(1, activos.size());
        assertFalse(activos.get(0).revoked);
        assertTrue(activos.get(0).expiresAt.isAfter(Instant.now()));
    }

    // ── revokeAllByUserId ─────────────────────────────────────────────────────

    @Test
    void revokeAllByUserId_revocaTodosLosTokensActivos() {
        User u = buildUser("rt_repo_revoke@test.com");
        buildToken(u, "hash_r1_" + UUID.randomUUID(), false, Instant.now().plusSeconds(3600));
        buildToken(u, "hash_r2_" + UUID.randomUUID(), false, Instant.now().plusSeconds(3600));
        RefreshToken.getEntityManager().flush();

        long updated = repo.revokeAllByUserId(u.id);
        RefreshToken.getEntityManager().flush();
        RefreshToken.getEntityManager().clear();

        assertEquals(2, updated);
        assertEquals(0, RefreshToken.count("user.id = ?1 and revoked = false", u.id));
    }

    @Test
    void revokeAllByUserId_noAfectaTokensYaRevocados() {
        User u = buildUser("rt_repo_revoke2@test.com");
        buildToken(u, "hash_ya_rev_" + UUID.randomUUID(), true, Instant.now().plusSeconds(3600));
        RefreshToken.getEntityManager().flush();

        long updated = repo.revokeAllByUserId(u.id);

        assertEquals(0, updated);
    }

    // ── deleteExpiredBefore ───────────────────────────────────────────────────

    @Test
    void deleteExpiredBefore_eliminaSoloTokensExpirados() {
        User u = buildUser("rt_repo_cleanup@test.com");
        buildToken(u, "hash_exp_" + UUID.randomUUID(), false, Instant.now().minusSeconds(120));
        buildToken(u, "hash_ok_"  + UUID.randomUUID(), false, Instant.now().plusSeconds(3600));
        RefreshToken.getEntityManager().flush();

        long deleted = repo.deleteExpiredBefore(Instant.now());

        assertEquals(1, deleted);
        assertEquals(1, RefreshToken.count("user.id", u.id));
    }
}
