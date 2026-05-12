package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class RoleRepositoryTest {

    @Inject
    RoleRepository repo;

    private Role buildRole(String name) {
        Role r = new Role();
        r.name = name;
        r.description = "Rol de prueba";
        r.persist();
        return r;
    }

    // ── findByName ────────────────────────────────────────────────────────────

    @Test
    void findByName_retornaRol() {
        buildRole("REPO_ROLE_FIND");

        Optional<Role> found = repo.findByName("REPO_ROLE_FIND");

        assertTrue(found.isPresent());
        assertEquals("REPO_ROLE_FIND", found.get().name);
    }

    @Test
    void findByName_retornaVacioParaNombreInexistente() {
        assertTrue(repo.findByName("INEXISTENTE").isEmpty());
    }

    @Test
    void findByName_retornaSeedRoles() {
        assertTrue(repo.findByName("SUPERUSER").isPresent());
        assertTrue(repo.findByName("ADMIN").isPresent());
        assertTrue(repo.findByName("DOCENTE").isPresent());
    }

    // ── existsByName ──────────────────────────────────────────────────────────

    @Test
    void existsByName_retornaTrueCuandoExiste() {
        buildRole("REPO_ROLE_EXISTS");
        assertTrue(repo.existsByName("REPO_ROLE_EXISTS"));
    }

    @Test
    void existsByName_retornaFalseCuandoNoExiste() {
        assertFalse(repo.existsByName("REPO_ROLE_NO_EXISTE"));
    }
}
