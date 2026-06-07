package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.auth.repository.RolePermissionRepository;
import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del algoritmo de autorización Unix-style (requisito 2). Es la fuente de
 * verdad única que materializa {@code @RequirePermission} y el endpoint público
 * {@code GET /auth/access}. Atómicos: el {@code RolePermissionRepository} (capa
 * de BD) está mockeado, de modo que se prueba exactamente la aritmética de bits
 * y el comodín {@code ALL}, sin tocar persistencia.
 *
 * <p>Contrato verificado:</p>
 * <ul>
 *   <li>flags efectivos = OR de los grants de todos los roles sobre el recurso;</li>
 *   <li>un grant sobre el comodín {@link ResourceIds#ALL} cubre cualquier recurso;</li>
 *   <li>{@code hasPermission} exige que TODOS los bits requeridos estén presentes
 *       ({@code (effective & required) == required}).</li>
 * </ul>
 */
class PermissionServiceTest {

    private RolePermissionRepository repository;
    private PermissionService service;

    private final List<UUID> roles = List.of(UUID.randomUUID(), UUID.randomUUID());

    @BeforeEach
    void setUp() {
        repository = mock(RolePermissionRepository.class);
        service = new PermissionService();
        service.permissionRepository = repository;
    }

    private RolePermission grant(short flags) {
        RolePermission rp = new RolePermission();
        rp.flags = flags;
        rp.resourceKey = AuthResourceId.USERS;
        return rp;
    }

    /** Stubs de las dos consultas que dispara effectiveFlags: recurso concreto y comodín ALL. */
    private void stub(String resourceKey, List<RolePermission> concrete, List<RolePermission> wildcard) {
        when(repository.findByRolesAndResource(anyList(), eq(resourceKey))).thenReturn(concrete);
        when(repository.findByRolesAndResource(anyList(), eq(ResourceIds.ALL))).thenReturn(wildcard);
    }

    @Test
    @DisplayName("Contrato de bits: r=4, w=2, x=1")
    void permissionBits_matchUnixContract() {
        assertEquals(4, Permission.READ.bit);
        assertEquals(2, Permission.WRITE.bit);
        assertEquals(1, Permission.EXECUTE.bit);
    }

    @Test
    @DisplayName("effectiveFlags de un único grant devuelve sus flags")
    void effectiveFlags_singleGrant() {
        stub(AuthResourceId.USERS, List.of(grant((short) 4)), List.of());
        assertEquals(4, service.effectiveFlags(roles, AuthResourceId.USERS));
    }

    @Test
    @DisplayName("effectiveFlags hace OR de los grants de varios roles sobre el mismo recurso")
    void effectiveFlags_orsMultipleGrants() {
        // r (4) de un rol + w (2) de otro ⇒ rw (6)
        stub(AuthResourceId.USERS, List.of(grant((short) 4), grant((short) 2)), List.of());
        assertEquals(6, service.effectiveFlags(roles, AuthResourceId.USERS));
    }

    @Test
    @DisplayName("effectiveFlags incluye el comodín ALL (patrón SUPERUSER)")
    void effectiveFlags_includesWildcard() {
        // Sin grant concreto, pero rwx (7) sobre ALL ⇒ 7 sobre cualquier recurso
        stub(AuthResourceId.USERS, List.of(), List.of(grant((short) 7)));
        assertEquals(7, service.effectiveFlags(roles, AuthResourceId.USERS));
    }

    @Test
    @DisplayName("effectiveFlags combina (OR) el grant concreto con el comodín ALL")
    void effectiveFlags_concreteOrWildcard() {
        // concreto r (4) + comodín x (1) ⇒ 5
        stub(AuthResourceId.USERS, List.of(grant((short) 4)), List.of(grant((short) 1)));
        assertEquals(5, service.effectiveFlags(roles, AuthResourceId.USERS));
    }

    @Test
    @DisplayName("hasPermission es true sólo si TODOS los bits requeridos están presentes")
    void hasPermission_requiresAllBits() {
        // efectivos = rw (6)
        stub(AuthResourceId.USERS, List.of(grant((short) 6)), List.of());

        assertTrue(service.hasPermission(roles, AuthResourceId.USERS, Permission.READ.bit));
        assertTrue(service.hasPermission(roles, AuthResourceId.USERS, Permission.WRITE.bit));
        assertFalse(service.hasPermission(roles, AuthResourceId.USERS, Permission.EXECUTE.bit),
            "rw no incluye x");
        // Requerir rwx (7) sobre efectivos rw (6) ⇒ falso
        assertFalse(service.hasPermission(roles, AuthResourceId.USERS, (short) 7));
    }

    @Test
    @DisplayName("hasPermission es false sin ningún grant (ni concreto ni comodín)")
    void hasPermission_noGrants_denied() {
        stub(AuthResourceId.USERS, List.of(), List.of());

        assertEquals(0, service.effectiveFlags(roles, AuthResourceId.USERS));
        assertFalse(service.hasPermission(roles, AuthResourceId.USERS, Permission.READ.bit));
        assertFalse(service.hasPermission(roles, AuthResourceId.USERS, Permission.WRITE.bit));
        assertFalse(service.hasPermission(roles, AuthResourceId.USERS, Permission.EXECUTE.bit));
    }

    @Test
    @DisplayName("hasPermission con cero roles ⇒ sin grants ⇒ denegado")
    void hasPermission_noRoles_denied() {
        when(repository.findByRolesAndResource(anyList(), eq(AuthResourceId.USERS))).thenReturn(List.of());
        when(repository.findByRolesAndResource(anyList(), eq(ResourceIds.ALL))).thenReturn(List.of());

        assertFalse(service.hasPermission(List.of(), AuthResourceId.USERS, Permission.READ.bit));
    }
}
