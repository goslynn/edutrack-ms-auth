package cl.duocuc.edutrack.ms.auth.model;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestTransaction;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class UserCrudTest {

    private User buildUser(String email) {
        User u = new User();
        u.email = email;
        u.passwordHash = "$2a$10$placeholder_hash";
        return u;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void persist_asignaIdYTimestamps() {
        User u = buildUser("create@test.com");
        u.persist();

        assertNotNull(u.id);
        assertNotNull(u.createdAt);
        assertNotNull(u.updatedAt);
        assertTrue(u.enabled, "enabled debe ser true por defecto");
    }

    @Test
    void persist_usuarioDeshabilitado() {
        User u = buildUser("disabled@test.com");
        u.enabled = false;
        u.persist();

        User found = User.findById(u.id);
        assertNotNull(found);
        assertFalse(found.enabled);
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void findById_retornaUsuarioPersistido() {
        User u = buildUser("findbyid@test.com");
        u.persist();

        User found = User.findById(u.id);
        assertNotNull(found);
        assertEquals("findbyid@test.com", found.email);
        assertEquals(u.id, found.id);
    }

    @Test
    void findById_retornaNullParaIdInexistente() {
        assertNull(User.findById(UUID.randomUUID()));
    }

    @Test
    void findByEmail_retornaUsuario() {
        User u = buildUser("byemail@test.com");
        u.persist();
        User.getEntityManager().flush();

        User found = User.<User>find("email", "byemail@test.com").firstResult();
        assertNotNull(found);
        assertEquals(u.id, found.id);
    }

    @Test
    void count_incrementaDespuesDePersistir() {
        long before = User.count();
        buildUser("count@test.com").persist();
        assertEquals(before + 1, User.count());
    }

    @Test
    void listAll_incluyeUsuarioPersistido() {
        User u = buildUser("list@test.com");
        u.persist();
        User.getEntityManager().flush();

        assertTrue(User.<User>listAll().stream().anyMatch(x -> x.id.equals(u.id)));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void update_cambiaEmailYActualizaTimestamp() {
        User u = buildUser("update@test.com");
        u.persist();
        User.getEntityManager().flush();

        u.email = "updated@test.com";
        User.getEntityManager().flush(); // dispara @PreUpdate

        User found = User.findById(u.id);
        assertEquals("updated@test.com", found.email);
        assertNotNull(found.updatedAt);
    }

    @Test
    void update_cambiaPasswordHash() {
        User u = buildUser("pwchange@test.com");
        u.persist();
        User.getEntityManager().flush();

        u.passwordHash = "$argon2id$nueva_hash";
        User.getEntityManager().flush();

        User found = User.findById(u.id);
        assertEquals("$argon2id$nueva_hash", found.passwordHash);
    }

    @Test
    void update_habilitarDeshabilitarUsuario() {
        User u = buildUser("toggle@test.com");
        u.persist();
        User.getEntityManager().flush();

        u.enabled = false;
        User.getEntityManager().flush();

        assertFalse(((User) User.findById(u.id)).enabled);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_eliminaUsuario() {
        User u = buildUser("delete@test.com");
        u.persist();
        User.getEntityManager().flush();
        UUID id = u.id;

        u.delete();

        assertNull(User.findById(id));
    }

    @Test
    void deleteById_eliminaUsuario() {
        User u = buildUser("deletebyid@test.com");
        u.persist();
        User.getEntityManager().flush();

        User.deleteById(u.id);

        assertNull(User.findById(u.id));
    }

    // ── CONSTRAINTS ───────────────────────────────────────────────────────────

    @Test
    void emailDuplicado_lanzaExcepcion() {
        User u1 = buildUser("dup@test.com");
        u1.persist();
        User.getEntityManager().flush();

        User u2 = buildUser("dup@test.com");
        u2.persist();

        assertThrows(PersistenceException.class, () -> User.getEntityManager().flush());
    }

    // ── CASCADE ───────────────────────────────────────────────────────────────

    @Test
    void deleteUser_eliminaRefreshTokensEnCascada() {
        User u = buildUser("cascade@test.com");
        u.persist();
        User.getEntityManager().flush();

        RefreshToken token = new RefreshToken();
        token.user = u;
        token.tokenHash = "hash_unico_cascade_test";
        token.expiresAt = java.time.Instant.now().plusSeconds(3600);
        token.persist();
        User.getEntityManager().flush();

        u.delete();

        assertEquals(0, RefreshToken.<RefreshToken>find("tokenHash", "hash_unico_cascade_test").count());
    }
}
