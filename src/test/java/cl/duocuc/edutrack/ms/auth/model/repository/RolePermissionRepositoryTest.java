package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.auth.repository.RolePermissionRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class RolePermissionRepositoryTest {

    @Inject
    RolePermissionRepository repo;

    private static String key() {
        return "test." + UUID.randomUUID();
    }

    private Role superuser() {
        return Role.<Role>find("name", "SUPERUSER").firstResult();
    }

    private Role admin() {
        return Role.<Role>find("name", "ADMIN").firstResult();
    }

    private RolePermission buildPerm(Role role, String resourceKey, short flags) {
        RolePermission p = new RolePermission();
        p.role = role;
        p.resourceKey = resourceKey;
        p.flags = flags;
        p.persist();
        return p;
    }

    // ── findByRoleAndResource ─────────────────────────────────────────────────

    @Test
    void findByRoleAndResource_retornaPermiso() {
        String resource = key();
        buildPerm(superuser(), resource, (short) 6);
        RolePermission.getEntityManager().flush();

        Optional<RolePermission> found = repo.findByRoleAndResource(superuser().id, resource);

        assertTrue(found.isPresent());
        assertEquals(6, found.get().flags);
    }

    @Test
    void findByRoleAndResource_retornaVacioSiNoExiste() {
        assertTrue(repo.findByRoleAndResource(superuser().id, key()).isEmpty());
    }

    // ── findByRoleId ──────────────────────────────────────────────────────────

    @Test
    void findByRoleId_retornaPermisosDelRol() {
        Role su = superuser();
        buildPerm(su, key(), (short) 4);
        buildPerm(su, key(), (short) 2);
        RolePermission.getEntityManager().flush();

        List<RolePermission> result = repo.findByRoleId(su.id);

        assertTrue(result.size() >= 2);
        assertTrue(result.stream().allMatch(p -> p.role.id.equals(su.id)));
    }

    // ── computeEffectiveFlags ─────────────────────────────────────────────────

    @Test
    void computeEffectiveFlags_unionDeFlagsDeMultiplesRoles() {
        String resource = key();
        buildPerm(superuser(), resource, (short) 4); // r
        buildPerm(admin(),     resource, (short) 2); // w
        RolePermission.getEntityManager().flush();

        short effective = repo.computeEffectiveFlags(
            List.of(superuser().id, admin().id), resource);

        assertEquals(6, effective); // r|w = 4|2 = 6
    }

    @Test
    void computeEffectiveFlags_sinRoles_retornaCero() {
        assertEquals(0, repo.computeEffectiveFlags(List.of(), key()));
    }

    @Test
    void computeEffectiveFlags_sinPermisosParaRecurso_retornaCero() {
        String resource = key();
        assertEquals(0, repo.computeEffectiveFlags(List.of(superuser().id), resource));
    }

    @Test
    void computeEffectiveFlags_unSoloRolConTodosLosFlags() {
        String resource = key();
        buildPerm(superuser(), resource, (short) 7);
        RolePermission.getEntityManager().flush();

        assertEquals(7, repo.computeEffectiveFlags(List.of(superuser().id), resource));
    }
}
