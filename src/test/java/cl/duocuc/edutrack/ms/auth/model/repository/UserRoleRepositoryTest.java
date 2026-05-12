package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.entity.UserRole;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class UserRoleRepositoryTest {

    @Inject
    UserRoleRepository repo;

    private User buildUser(String email) {
        User u = new User();
        u.displayName = "Test User";
        u.email = email;
        u.passwordHash = "$2a$10$placeholder";
        u.persist();
        return u;
    }

    private Role superuser() {
        return Role.<Role>find("name", "SUPERUSER").firstResult();
    }

    private Role admin() {
        return Role.<Role>find("name", "ADMIN").firstResult();
    }

    // ── findByUserId ──────────────────────────────────────────────────────────

    @Test
    void findByUserId_retornaAsignacionesDelUsuario() {
        User u = buildUser("ur_repo_find@test.com");
        new UserRole(u, superuser()).persist();
        new UserRole(u, admin()).persist();
        UserRole.getEntityManager().flush();

        List<UserRole> result = repo.findByUserId(u.id);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ur -> ur.id.userId.equals(u.id)));
    }

    @Test
    void findByUserId_retornaVacioSinAsignaciones() {
        User u = buildUser("ur_repo_empty@test.com");
        assertTrue(repo.findByUserId(u.id).isEmpty());
    }

    // ── findRoleIdsByUserId ───────────────────────────────────────────────────

    @Test
    void findRoleIdsByUserId_retornaUUIDsDeRoles() {
        User u = buildUser("ur_repo_roleids@test.com");
        Role su = superuser();
        new UserRole(u, su).persist();
        UserRole.getEntityManager().flush();

        List<UUID> roleIds = repo.findRoleIdsByUserId(u.id);

        assertEquals(1, roleIds.size());
        assertEquals(su.id, roleIds.get(0));
    }

    // ── existsAssignment ──────────────────────────────────────────────────────

    @Test
    void existsAssignment_retornaTrueCuandoAsignado() {
        User u = buildUser("ur_repo_exists@test.com");
        Role su = superuser();
        new UserRole(u, su).persist();
        UserRole.getEntityManager().flush();

        assertTrue(repo.existsAssignment(u.id, su.id));
    }

    @Test
    void existsAssignment_retornaFalseCuandoNoAsignado() {
        User u = buildUser("ur_repo_notexists@test.com");
        assertFalse(repo.existsAssignment(u.id, superuser().id));
    }

    // ── deleteByUserId ────────────────────────────────────────────────────────

    @Test
    void deleteByUserId_eliminaTodasLasAsignaciones() {
        User u = buildUser("ur_repo_delete@test.com");
        new UserRole(u, superuser()).persist();
        new UserRole(u, admin()).persist();
        UserRole.getEntityManager().flush();

        long deleted = repo.deleteByUserId(u.id);

        assertEquals(2, deleted);
        assertTrue(repo.findByUserId(u.id).isEmpty());
    }
}
