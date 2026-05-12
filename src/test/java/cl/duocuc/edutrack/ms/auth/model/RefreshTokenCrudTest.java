package cl.duocuc.edutrack.ms.auth.model;

import cl.duocuc.edutrack.ms.auth.model.entity.RefreshToken;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class RefreshTokenCrudTest {

    private User buildUser(String email) {
        User u = new User();
        u.displayName = "Test User";
        u.email = email;
        u.passwordHash = "$2a$10$placeholder";
        u.persist();
        return u;
    }

    private RefreshToken buildToken(User user, String hash) {
        RefreshToken t = new RefreshToken();
        t.user = user;
        t.tokenHash = hash;
        t.expiresAt = Instant.now().plusSeconds(3600);
        return t;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void persist_asignaIdYCreatedAt() {
        User u = buildUser("rt_create@test.com");
        RefreshToken t = buildToken(u, "hash_create_test_" + UUID.randomUUID());
        t.persist();

        assertNotNull(t.id);
        assertNotNull(t.createdAt);
        assertFalse(t.revoked, "revoked debe ser false por defecto");
        assertNull(t.revokedAt);
    }

    @Test
    void persist_soloAlmacenaHash_noTokenRaw() {
        User u = buildUser("rt_hash@test.com");
        String hash = "sha256_hash_" + UUID.randomUUID();
        buildToken(u, hash).persist();
        RefreshToken.getEntityManager().flush();

        RefreshToken found = RefreshToken.<RefreshToken>find("tokenHash", hash).firstResult();
        assertNotNull(found);
        assertEquals(hash, found.tokenHash);
    }

    @Test
    void persist_tokenExpirado_guardaFechaExpiracion() {
        User u = buildUser("rt_expire@test.com");
        Instant exp = Instant.now().minusSeconds(60);
        RefreshToken t = buildToken(u, "hash_expired_" + UUID.randomUUID());
        t.expiresAt = exp;
        t.persist();

        RefreshToken found = RefreshToken.findById(t.id);
        assertTrue(found.expiresAt.isBefore(Instant.now()));
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void findById_retornaToken() {
        User u = buildUser("rt_find@test.com");
        RefreshToken t = buildToken(u, "hash_find_" + UUID.randomUUID());
        t.persist();

        RefreshToken found = RefreshToken.findById(t.id);
        assertNotNull(found);
        assertEquals(t.tokenHash, found.tokenHash);
    }

    @Test
    void findById_retornaNullParaIdInexistente() {
        assertNull(RefreshToken.findById(UUID.randomUUID()));
    }

    @Test
    void findByTokenHash_retornaToken() {
        User u = buildUser("rt_byhash@test.com");
        String hash = "hash_bytoken_" + UUID.randomUUID();
        buildToken(u, hash).persist();
        RefreshToken.getEntityManager().flush();

        RefreshToken found = RefreshToken.<RefreshToken>find("tokenHash", hash).firstResult();
        assertNotNull(found);
    }

    @Test
    void findTokensActivos_excluirRevocados() {
        User u = buildUser("rt_active@test.com");
        String hash = "hash_active_" + UUID.randomUUID();
        RefreshToken active = buildToken(u, hash);
        active.persist();

        String hashRevoked = "hash_revoked_" + UUID.randomUUID();
        RefreshToken revoked = buildToken(u, hashRevoked);
        revoked.revoked = true;
        revoked.revokedAt = Instant.now();
        revoked.persist();
        RefreshToken.getEntityManager().flush();

        long activos = RefreshToken.count("user.id = ?1 and revoked = false", u.id);
        long revocados = RefreshToken.count("user.id = ?1 and revoked = true", u.id);

        assertEquals(1, activos);
        assertEquals(1, revocados);
    }

    @Test
    void count_incrementaDespuesDePersistir() {
        User u = buildUser("rt_count@test.com");
        long before = RefreshToken.count();
        buildToken(u, "hash_count_" + UUID.randomUUID()).persist();
        assertEquals(before + 1, RefreshToken.count());
    }

    // ── UPDATE (revocación) ───────────────────────────────────────────────────

    @Test
    void revocarToken_setsRevokedYRevokedAt() {
        User u = buildUser("rt_revoke@test.com");
        RefreshToken t = buildToken(u, "hash_revoke_" + UUID.randomUUID());
        t.persist();
        RefreshToken.getEntityManager().flush();

        t.revoked = true;
        t.revokedAt = Instant.now();
        RefreshToken.getEntityManager().flush();

        RefreshToken found = RefreshToken.findById(t.id);
        assertTrue(found.revoked);
        assertNotNull(found.revokedAt);
    }

    @Test
    void renovarExpiracion_actualizaExpiresAt() {
        User u = buildUser("rt_renew@test.com");
        RefreshToken t = buildToken(u, "hash_renew_" + UUID.randomUUID());
        t.persist();
        RefreshToken.getEntityManager().flush();

        Instant nuevaExp = Instant.now().plusSeconds(7200);
        t.expiresAt = nuevaExp;
        RefreshToken.getEntityManager().flush();

        RefreshToken found = RefreshToken.findById(t.id);
        assertEquals(nuevaExp.getEpochSecond(), found.expiresAt.getEpochSecond());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_eliminaToken() {
        User u = buildUser("rt_delete@test.com");
        RefreshToken t = buildToken(u, "hash_delete_" + UUID.randomUUID());
        t.persist();
        RefreshToken.getEntityManager().flush();
        UUID id = t.id;

        t.delete();

        assertNull(RefreshToken.findById(id));
    }

    @Test
    void deleteById_eliminaToken() {
        User u = buildUser("rt_deletebyid@test.com");
        RefreshToken t = buildToken(u, "hash_deletebyid_" + UUID.randomUUID());
        t.persist();
        RefreshToken.getEntityManager().flush();

        RefreshToken.deleteById(t.id);

        assertNull(RefreshToken.findById(t.id));
    }

    @Test
    void deleteUsuario_eliminaTokensEnCascada() {
        User u = buildUser("rt_cascade@test.com");
        String hash = "hash_cascade_" + UUID.randomUUID();
        buildToken(u, hash).persist();
        RefreshToken.getEntityManager().flush();

        User.getEntityManager().refresh(u); // sustituye el ArrayList plain por el proxy Hibernate
        int size = u.refreshTokens.size();             // inicializa la colección lazy desde DB
        assertEquals(1, size, "debería haber 1 token asociado al usuario");

        u.delete();
        User.getEntityManager().flush();
        User.getEntityManager().clear();

        assertEquals(0, RefreshToken.count("tokenHash", hash));
    }

    // ── CONSTRAINTS ───────────────────────────────────────────────────────────

    @Test
    void tokenHashDuplicado_lanzaExcepcion() {
        User u = buildUser("rt_dup@test.com");
        String hash = "hash_dup_" + UUID.randomUUID();

        buildToken(u, hash).persist();
        RefreshToken.getEntityManager().flush();

        buildToken(u, hash).persist();

        assertThrows(PersistenceException.class,
            () -> RefreshToken.getEntityManager().flush());
    }
}
