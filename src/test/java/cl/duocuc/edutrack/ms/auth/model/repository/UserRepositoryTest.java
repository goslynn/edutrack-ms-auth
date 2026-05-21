package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class UserRepositoryTest {

    @Inject
    UserRepository repo;

    private User buildUser(String email) {
        User u = new User();
        u.displayName = "Test User";
        u.email = email;
        u.passwordHash = "$2a$10$placeholder";
        u.persist();
        return u;
    }

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    void findByEmail_retornaUsuario() {
        buildUser("repo_user_find@test.com");

        Optional<User> found = repo.findByEmail("repo_user_find@test.com");

        assertTrue(found.isPresent());
        assertEquals("repo_user_find@test.com", found.get().email);
    }

    @Test
    void findByEmail_retornaVacioParaEmailInexistente() {
        assertTrue(repo.findByEmail("no_existe@test.com").isEmpty());
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    void existsByEmail_retornaTrueCuandoExiste() {
        buildUser("repo_user_exists@test.com");
        assertTrue(repo.existsByEmail("repo_user_exists@test.com"));
    }

    @Test
    void existsByEmail_retornaFalseCuandoNoExiste() {
        assertFalse(repo.existsByEmail("no_existe@test.com"));
    }

    // ── listEnabled ───────────────────────────────────────────────────────────

    @Test
    void listEnabled_excluyeUsuariosDeshabilitados() {
        User enabled = buildUser("repo_user_enabled@test.com");

        User disabled = new User();
        disabled.displayName = "Disabled";
        disabled.email = "repo_user_disabled@test.com";
        disabled.passwordHash = "$2a$10$placeholder";
        disabled.enabled = false;
        disabled.persist();

        var list = repo.listEnabled();

        assertTrue(list.stream().anyMatch(u -> u.id.equals(enabled.id)));
        assertTrue(list.stream().noneMatch(u -> u.id.equals(disabled.id)));
    }
}
