package cl.duocuc.edutrack.ms.auth.db;

import cl.duocuc.edutrack.ms.auth.model.entity.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DatabaseConnectivityTest {

    @Inject
    EntityManager em;

    @Test
    void entityManagerIsOpen() {
        assertNotNull(em);
        assertTrue(em.isOpen());
    }

    @Test
    @TestTransaction
    void allPanacheEntitiesAreAccessible() {
        assertDoesNotThrow(() -> User.count());
        assertDoesNotThrow(() -> Role.count());
        assertDoesNotThrow(() -> UserRole.count());
        assertDoesNotThrow(() -> RolePermission.count());
        assertDoesNotThrow(() -> RefreshToken.count());
    }

    @Test
    @TestTransaction
    void flywayCreadoTodasLasTablas() {
        @SuppressWarnings("unchecked")
        List<String> tables = em.createNativeQuery(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'auth' ORDER BY tablename"
        ).getResultList();

        assertTrue(tables.contains("users"), "tabla users no encontrada");
        assertTrue(tables.contains("roles"), "tabla roles no encontrada");
        assertTrue(tables.contains("user_roles"), "tabla user_roles no encontrada");
        assertTrue(tables.contains("role_permissions"), "tabla role_permissions no encontrada");
        assertTrue(tables.contains("refresh_tokens"), "tabla refresh_tokens no encontrada");
    }

    @Test
    @TestTransaction
    void flywayV2SeedCargaRoles() {
        assertEquals(3, Role.count(), "V2 seed debe tener exactamente SUPERUSER, ADMIN y DOCENTE");
        assertNotNull(Role.find("name", "SUPERUSER").firstResult());
        assertNotNull(Role.find("name", "ADMIN").firstResult());
        assertNotNull(Role.find("name", "DOCENTE").firstResult());
    }

    @Test
    @TestTransaction
    void indicesPgExisten() {
        @SuppressWarnings("unchecked")
        List<String> indexes = em.createNativeQuery(
            "SELECT indexname FROM pg_indexes WHERE schemaname = 'auth'"
        ).getResultList();

        assertTrue(indexes.contains("idx_user_roles_user_id"));
        assertTrue(indexes.contains("idx_role_permissions_role_id"));
        assertTrue(indexes.contains("idx_refresh_tokens_user_id"));
        assertTrue(indexes.contains("idx_refresh_tokens_token_hash"));
    }
}
