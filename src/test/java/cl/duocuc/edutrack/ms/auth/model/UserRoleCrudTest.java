package cl.duocuc.edutrack.ms.auth.model;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class UserRoleCrudTest {

    private User buildUser(String email) {
        User u = new User();
        u.email = email;
        u.passwordHash = "$2a$10$placeholder";
        return u;
    }

    private Role superuser() {
        return Role.<Role>find("name", "SUPERUSER").firstResult();
    }

    private Role admin() {
        return Role.<Role>find("name", "ADMIN").firstResult();
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void asignarRol_persisteUserRole() {
        User u = buildUser("ur_create@test.com");
        u.persist();
        Role.getEntityManager().flush();

        UserRole ur = new UserRole(u, superuser());
        ur.persist();
        UserRole.getEntityManager().flush();

        assertNotNull(ur.id);
        assertNotNull(ur.assignedAt);
        assertEquals(u.id, ur.id.userId);
        assertEquals(superuser().id, ur.id.roleId);
    }

    @Test
    void asignarMultiplesRoles_persisten() {
        User u = buildUser("ur_multi@test.com");
        u.persist();
        UserRole.getEntityManager().flush();

        new UserRole(u, superuser()).persist();
        new UserRole(u, admin()).persist();
        UserRole.getEntityManager().flush();

        long count = UserRole.count("id.userId", u.id);
        assertEquals(2, count);
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void findById_usandoPKCompuesta() {
        User u = buildUser("ur_find@test.com");
        u.persist();
        Role r = superuser();
        UserRole.getEntityManager().flush();

        UserRole ur = new UserRole(u, r);
        ur.persist();
        UserRole.getEntityManager().flush();

        UserRole found = UserRole.findById(new UserRoleId(u.id, r.id));
        assertNotNull(found);
        assertEquals(u.id, found.id.userId);
        assertEquals(r.id, found.id.roleId);
    }

    @Test
    void findRolesPorUsuario() {
        User u = buildUser("ur_query@test.com");
        u.persist();
        UserRole.getEntityManager().flush();

        new UserRole(u, superuser()).persist();
        UserRole.getEntityManager().flush();

        long count = UserRole.count("id.userId", u.id);
        assertEquals(1, count);
    }

    @Test
    void count_incrementaDespuesDeAsignar() {
        User u = buildUser("ur_count@test.com");
        u.persist();
        UserRole.getEntityManager().flush();

        long before = UserRole.count();
        new UserRole(u, superuser()).persist();
        UserRole.getEntityManager().flush();

        assertEquals(before + 1, UserRole.count());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void revocarRol_eliminaUserRole() {
        User u = buildUser("ur_delete@test.com");
        u.persist();
        Role r = superuser();
        UserRole.getEntityManager().flush();

        UserRole ur = new UserRole(u, r);
        ur.persist();
        UserRole.getEntityManager().flush();

        ur.delete();

        assertNull(UserRole.findById(new UserRoleId(u.id, r.id)));
    }

    @Test
    void deleteUsuario_eliminaUserRolesEnCascada() {
        User u = buildUser("ur_cascade@test.com");
        u.persist();
        Role r = superuser();
        UserRole.getEntityManager().flush();

        new UserRole(u, r).persist();
        UserRole.getEntityManager().flush();

        UserRoleId pk = new UserRoleId(u.id, r.id);
        UserRole.getEntityManager().refresh(u); // sustituye el ArrayList plain por el proxy Hibernate
        int size = u.userRoles.size();                     // inicializa la colección lazy desde DB
        assertEquals(1, size, "debería haber 1 UserRole asociado al usuario");
        u.delete();
        UserRole.getEntityManager().flush();
        UserRole.getEntityManager().clear();

        assertNull(UserRole.findById(pk));
    }

    // ── CONSTRAINTS ───────────────────────────────────────────────────────────

    @Test
    void userRoleId_equalsHashCode_mismosPares() {
        UserRoleId id1 = new UserRoleId(
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
            java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")
        );
        UserRoleId id2 = new UserRoleId(
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
            java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")
        );

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void userRoleId_notEquals_paresDiferentes() {
        UserRoleId id1 = new UserRoleId(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID()
        );
        UserRoleId id2 = new UserRoleId(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID()
        );

        assertNotEquals(id1, id2);
    }
}
