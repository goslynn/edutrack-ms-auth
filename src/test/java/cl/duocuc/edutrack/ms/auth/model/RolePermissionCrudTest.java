package cl.duocuc.edutrack.ms.auth.model;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class RolePermissionCrudTest {

    private static String key() {
        return "test." + UUID.randomUUID();
    }

    private Role superuser() {
        return Role.<Role>find("name", "SUPERUSER").firstResult();
    }

    private RolePermission buildPerm(Role role, String resourceKey, short flags) {
        RolePermission p = new RolePermission();
        p.role = role;
        p.resourceKey = resourceKey;
        p.flags = flags;
        return p;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void persist_flagsRead_asignaIdYTimestamps() {
        RolePermission p = buildPerm(superuser(), key(), (short) 4);
        p.persist();

        assertNotNull(p.id);
        assertNotNull(p.createdAt);
        assertNotNull(p.updatedAt);
        assertEquals(4, p.flags);
    }

    @Test
    void persist_flagsWrite() {
        RolePermission p = buildPerm(superuser(), key(), (short) 2);
        p.persist();

        assertEquals(2, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void persist_flagsExecute() {
        RolePermission p = buildPerm(superuser(), key(), (short) 1);
        p.persist();

        assertEquals(1, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void persist_flagsReadWriteExecute() {
        RolePermission p = buildPerm(superuser(), key(), (short) 7);
        p.persist();

        assertEquals(7, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void persist_flagsCero_sinPermisos() {
        RolePermission p = buildPerm(superuser(), key(), (short) 0);
        p.persist();

        assertEquals(0, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void resourceKeyEsOpaco_sinFKAOtraTabla() {
        // Guarda una clave arbitraria — no requiere existencia en otra tabla
        String arbitrary = "arbitrary.opaque.key";
        RolePermission p = buildPerm(superuser(), arbitrary, (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        RolePermission found = RolePermission.findById(p.id);
        assertEquals(arbitrary, found.resourceKey);
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void findById_retornaPermiso() {
        String resource = key();
        RolePermission p = buildPerm(superuser(), resource, (short) 6);
        p.persist();

        RolePermission found = RolePermission.findById(p.id);
        assertNotNull(found);
        assertEquals(resource, found.resourceKey);
        assertEquals(6, found.flags);
    }

    @Test
    void findPorRol_retornaPermisoDelRol() {
        Role r = superuser();
        String resource = key();
        buildPerm(r, resource, (short) 5).persist();
        RolePermission.getEntityManager().flush();

        RolePermission found = RolePermission
            .<RolePermission>find("role.id = ?1 and resourceKey = ?2", r.id, resource)
            .firstResult();

        assertNotNull(found);
        assertEquals(5, found.flags);
    }

    @Test
    void count_incrementaDespuesDePersistir() {
        long before = RolePermission.count();
        buildPerm(superuser(), key(), (short) 4).persist();
        assertEquals(before + 1, RolePermission.count());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void update_cambiaFlags() {
        RolePermission p = buildPerm(superuser(), key(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        p.flags = 7;
        RolePermission.getEntityManager().flush(); // dispara @PreUpdate

        assertEquals(7, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void update_actualizaUpdatedAt() {
        RolePermission p = buildPerm(superuser(), key(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        p.flags = 2;
        RolePermission.getEntityManager().flush();

        assertNotNull(((RolePermission) RolePermission.findById(p.id)).updatedAt);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_eliminaPermiso() {
        RolePermission p = buildPerm(superuser(), key(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();
        UUID id = p.id;

        p.delete();

        assertNull(RolePermission.findById(id));
    }

    @Test
    void deleteById_eliminaPermiso() {
        RolePermission p = buildPerm(superuser(), key(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        RolePermission.deleteById(p.id);

        assertNull(RolePermission.findById(p.id));
    }

    // ── CONSTRAINTS ───────────────────────────────────────────────────────────

    @Test
    void duplicadoRolRecurso_lanzaExcepcion() {
        String resource = key();
        Role r = superuser();

        buildPerm(r, resource, (short) 4).persist();
        RolePermission.getEntityManager().flush();

        buildPerm(r, resource, (short) 2).persist();

        assertThrows(PersistenceException.class,
            () -> RolePermission.getEntityManager().flush());
    }

    @Test
    void mismoRecursoDistintoRol_esValido() {
        String resource = key();

        buildPerm(superuser(), resource, (short) 7).persist();
        buildPerm(Role.<Role>find("name", "ADMIN").firstResult(), resource, (short) 4).persist();

        assertDoesNotThrow(() -> RolePermission.getEntityManager().flush());
    }
}
