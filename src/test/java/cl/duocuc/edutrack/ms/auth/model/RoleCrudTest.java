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
class RoleCrudTest {

    private Role buildRole(String name) {
        Role r = new Role();
        r.name = name;
        r.description = "Rol de prueba: " + name;
        return r;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void persist_asignaIdYTimestamps() {
        Role r = buildRole("TEST_CREATE");
        r.persist();

        assertNotNull(r.id);
        assertNotNull(r.createdAt);
        assertNotNull(r.updatedAt);
    }

    @Test
    void persist_sinDescripcion() {
        Role r = new Role();
        r.name = "TEST_NO_DESC";
        r.persist();

        Role found = Role.findById(r.id);
        assertNotNull(found);
        assertNull(found.description);
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void findById_retornaRolPersistido() {
        Role r = buildRole("TEST_FIND");
        r.persist();

        Role found = Role.findById(r.id);
        assertNotNull(found);
        assertEquals("TEST_FIND", found.name);
    }

    @Test
    void findById_retornaNullParaIdInexistente() {
        assertNull(Role.findById(UUID.randomUUID()));
    }

    @Test
    void findByName_retornaRol() {
        Role r = buildRole("TEST_BYNAME");
        r.persist();
        Role.getEntityManager().flush();

        Role found = Role.<Role>find("name", "TEST_BYNAME").firstResult();
        assertNotNull(found);
        assertEquals(r.id, found.id);
    }

    @Test
    void seedData_superuserAdminDocenteExisten() {
        assertNotNull(Role.<Role>find("name", "SUPERUSER").firstResult());
        assertNotNull(Role.<Role>find("name", "ADMIN").firstResult());
        assertNotNull(Role.<Role>find("name", "DOCENTE").firstResult());
    }

    @Test
    void count_incluyeSeedData() {
        long seed = Role.count();
        assertTrue(seed >= 3, "Deben existir al menos los 3 roles del seed");
    }

    @Test
    void listAll_incluyeRolPersistido() {
        Role r = buildRole("TEST_LIST");
        r.persist();
        Role.getEntityManager().flush();

        assertTrue(Role.<Role>listAll().stream().anyMatch(x -> x.id.equals(r.id)));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void update_cambiaDescripcionYActualizaTimestamp() {
        Role r = buildRole("TEST_UPDATE");
        r.persist();
        Role.getEntityManager().flush();

        r.description = "Descripción actualizada";
        Role.getEntityManager().flush(); // dispara @PreUpdate

        Role found = Role.findById(r.id);
        assertEquals("Descripción actualizada", found.description);
        assertNotNull(found.updatedAt);
    }

    @Test
    void update_cambiaNombre() {
        Role r = buildRole("TEST_OLD_NAME");
        r.persist();
        Role.getEntityManager().flush();

        r.name = "TEST_NEW_NAME";
        Role.getEntityManager().flush();

        assertEquals("TEST_NEW_NAME", ((Role) Role.findById(r.id)).name);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_eliminaRol() {
        Role r = buildRole("TEST_DELETE");
        r.persist();
        Role.getEntityManager().flush();
        UUID id = r.id;

        r.delete();

        assertNull(Role.findById(id));
    }

    @Test
    void deleteById_eliminaRol() {
        Role r = buildRole("TEST_DELETEBYID");
        r.persist();
        Role.getEntityManager().flush();

        Role.deleteById(r.id);

        assertNull(Role.findById(r.id));
    }

    // ── CONSTRAINTS ───────────────────────────────────────────────────────────

    @Test
    void nombreDuplicado_lanzaExcepcion() {
        buildRole("TEST_DUP").persist();
        Role.getEntityManager().flush();

        buildRole("TEST_DUP").persist();

        assertThrows(PersistenceException.class, () -> Role.getEntityManager().flush());
    }

    // ── CASCADE ───────────────────────────────────────────────────────────────

    @Test
    void deleteRole_eliminaPermisosEnCascada() {
        Role r = buildRole("TEST_PERM_CASCADE");
        r.persist();
        Role.getEntityManager().flush();

        RolePermission perm = new RolePermission();
        perm.role = r;
        perm.resourceUuid = UUID.randomUUID();
        perm.flags = 4;
        perm.persist();
        Role.getEntityManager().flush();
        UUID permId = perm.id;

        Role.getEntityManager().refresh(r); // sustituye el ArrayList plain por el proxy Hibernate
        int size = r.permissions.size();               // inicializa la colección lazy desde DB
        assertEquals(1, size, "debería haber 1 permiso asociado al rol");
        r.delete();
        Role.getEntityManager().flush();
        Role.getEntityManager().clear();

        assertNull(RolePermission.findById(permId));
    }
}
