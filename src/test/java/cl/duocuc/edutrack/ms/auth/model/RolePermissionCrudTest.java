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

    private Role superuser() {
        return Role.<Role>find("name", "SUPERUSER").firstResult();
    }

    private RolePermission buildPerm(Role role, UUID resourceUuid, short flags) {
        RolePermission p = new RolePermission();
        p.role = role;
        p.resourceUuid = resourceUuid;
        p.flags = flags;
        return p;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void persist_flagsRead_asignaIdYTimestamps() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 4);
        p.persist();

        assertNotNull(p.id);
        assertNotNull(p.createdAt);
        assertNotNull(p.updatedAt);
        assertEquals(4, p.flags);
    }

    @Test
    void persist_flagsWrite() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 2);
        p.persist();

        assertEquals(2, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void persist_flagsExecute() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 1);
        p.persist();

        assertEquals(1, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void persist_flagsReadWriteExecute() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 7);
        p.persist();

        assertEquals(7, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void persist_flagsCero_sinPermisos() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 0);
        p.persist();

        assertEquals(0, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void resourceUuidEsOpaco_sinFKAOtraTabla() {
        // Guarda un UUID arbitrario — no requiere existencia en otra tabla
        UUID arbitrary = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        RolePermission p = buildPerm(superuser(), arbitrary, (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        RolePermission found = RolePermission.findById(p.id);
        assertEquals(arbitrary, found.resourceUuid);
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void findById_retornaPermiso() {
        UUID resource = UUID.randomUUID();
        RolePermission p = buildPerm(superuser(), resource, (short) 6);
        p.persist();

        RolePermission found = RolePermission.findById(p.id);
        assertNotNull(found);
        assertEquals(resource, found.resourceUuid);
        assertEquals(6, found.flags);
    }

    @Test
    void findPorRol_retornaPermisoDelRol() {
        Role r = superuser();
        UUID resource = UUID.randomUUID();
        buildPerm(r, resource, (short) 5).persist();
        RolePermission.getEntityManager().flush();

        RolePermission found = RolePermission
            .<RolePermission>find("role.id = ?1 and resourceUuid = ?2", r.id, resource)
            .firstResult();

        assertNotNull(found);
        assertEquals(5, found.flags);
    }

    @Test
    void count_incrementaDespuesDePersistir() {
        long before = RolePermission.count();
        buildPerm(superuser(), UUID.randomUUID(), (short) 4).persist();
        assertEquals(before + 1, RolePermission.count());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void update_cambiaFlags() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        p.flags = 7;
        RolePermission.getEntityManager().flush(); // dispara @PreUpdate

        assertEquals(7, ((RolePermission) RolePermission.findById(p.id)).flags);
    }

    @Test
    void update_actualizaUpdatedAt() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        p.flags = 2;
        RolePermission.getEntityManager().flush();

        assertNotNull(((RolePermission) RolePermission.findById(p.id)).updatedAt);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_eliminaPermiso() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();
        UUID id = p.id;

        p.delete();

        assertNull(RolePermission.findById(id));
    }

    @Test
    void deleteById_eliminaPermiso() {
        RolePermission p = buildPerm(superuser(), UUID.randomUUID(), (short) 4);
        p.persist();
        RolePermission.getEntityManager().flush();

        RolePermission.deleteById(p.id);

        assertNull(RolePermission.findById(p.id));
    }

    // ── CONSTRAINTS ───────────────────────────────────────────────────────────

    @Test
    void duplicadoRolRecurso_lanzaExcepcion() {
        UUID resource = UUID.randomUUID();
        Role r = superuser();

        buildPerm(r, resource, (short) 4).persist();
        RolePermission.getEntityManager().flush();

        buildPerm(r, resource, (short) 2).persist();

        assertThrows(PersistenceException.class,
            () -> RolePermission.getEntityManager().flush());
    }

    @Test
    void mismoRecursoDistintoRol_esValido() {
        UUID resource = UUID.randomUUID();

        buildPerm(superuser(), resource, (short) 7).persist();
        buildPerm(Role.<Role>find("name", "ADMIN").firstResult(), resource, (short) 4).persist();

        assertDoesNotThrow(() -> RolePermission.getEntityManager().flush());
    }
}
