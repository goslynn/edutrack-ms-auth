package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.auth.repository.RolePermissionRepository;
import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Matriz de consistencia usuarios × roles × recursos (requisito 3). Distintos
 * usuarios con distintas combinaciones de roles intentan acceder a recursos con
 * distintos niveles de permiso requerido; se valida que la decisión Unix-style
 * sea correcta en cada celda.
 *
 * <p>El {@code RolePermissionRepository} (capa de BD) se simula con un fixture
 * en memoria: para cualquier {@code (roleIds, resourceKey)} devuelve los grants
 * que cada rol tiene sobre ese recurso, de modo que corre el algoritmo REAL de
 * {@link PermissionService} —incluido el OR entre roles y el comodín {@code ALL}—
 * sin tocar persistencia. Así la matriz es agnóstica del almacenamiento y
 * reproduce fielmente cómo se acumulan los permisos de un usuario multi-rol.</p>
 *
 * <p>Arquetipos de rol:</p>
 * <ul>
 *   <li><b>SUPERUSER</b>: grant {@code rwx} sobre el comodín {@code ALL} — cubre todo recurso.</li>
 *   <li><b>ADMIN</b>: {@code rwx} sobre los recursos de Auth, pero SIN comodín.</li>
 *   <li><b>DOCENTE</b>: sólo lectura sobre {@code auth.permissions.effective}.</li>
 *   <li><b>READER</b> / <b>WRITER</b>: un único bit (r / w) sobre {@code auth.users} — para probar el OR.</li>
 * </ul>
 */
class PermissionMatrixTest {

    // Identidad estable de cada rol arquetípico.
    private static final UUID SUPERUSER = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN     = UUID.fromString("ad000000-0000-0000-0000-000000000001");
    private static final UUID DOCENTE   = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID READER    = UUID.fromString("4e000000-0000-0000-0000-000000000001");
    private static final UUID WRITER    = UUID.fromString("31000000-0000-0000-0000-000000000001");

    private static final short R = 4, W = 2, X = 1, RWX = 7;

    // Fixture: rol -> (resourceKey -> flags). Simula la tabla auth.role_permissions.
    private static final Map<UUID, Map<String, Short>> GRANTS = new HashMap<>();
    static {
        GRANTS.put(SUPERUSER, Map.of(ResourceIds.ALL, RWX));
        GRANTS.put(ADMIN, Map.of(
            AuthResourceId.USERS, RWX,
            AuthResourceId.ROLES, RWX,
            AuthResourceId.PERMISSIONS, RWX,
            AuthResourceId.USER_ROLES, RWX));
        GRANTS.put(DOCENTE, Map.of(AuthResourceId.PERMISSIONS_EFFECTIVE, R));
        GRANTS.put(READER, Map.of(AuthResourceId.USERS, R));
        GRANTS.put(WRITER, Map.of(AuthResourceId.USERS, W));
    }

    private PermissionService service;

    @BeforeEach
    void setUp() {
        RolePermissionRepository repository = mock(RolePermissionRepository.class);
        // Responde como lo haría la BD: por cada rol que tenga un grant sobre el
        // recurso pedido, devuelve una fila RolePermission con esos flags.
        when(repository.findByRolesAndResource(anyList(), anyString())).thenAnswer(inv -> {
            List<UUID> roleIds = inv.getArgument(0);
            String resourceKey = inv.getArgument(1);
            List<RolePermission> rows = new ArrayList<>();
            for (UUID roleId : roleIds) {
                Short flags = GRANTS.getOrDefault(roleId, Map.of()).get(resourceKey);
                if (flags != null) {
                    RolePermission rp = new RolePermission();
                    rp.flags = flags;
                    rp.resourceKey = resourceKey;
                    rows.add(rp);
                }
            }
            return rows;
        });
        service = new PermissionService();
        service.permissionRepository = repository;
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
            // --- SUPERUSER: el comodín ALL cubre cualquier recurso y cualquier bit ---
            scenario("SUPERUSER lee auth.users", List.of(SUPERUSER), AuthResourceId.USERS, Permission.READ, true),
            scenario("SUPERUSER escribe auth.users", List.of(SUPERUSER), AuthResourceId.USERS, Permission.WRITE, true),
            scenario("SUPERUSER ejecuta auth.roles", List.of(SUPERUSER), AuthResourceId.ROLES, Permission.EXECUTE, true),
            scenario("SUPERUSER accede a un recurso de OTRO MS vía ALL", List.of(SUPERUSER), "course.grades", Permission.WRITE, true),

            // --- ADMIN: rwx sobre recursos de Auth, pero sin comodín ---
            scenario("ADMIN escribe auth.users", List.of(ADMIN), AuthResourceId.USERS, Permission.WRITE, true),
            scenario("ADMIN lee auth.roles", List.of(ADMIN), AuthResourceId.ROLES, Permission.READ, true),
            scenario("ADMIN NO accede a recursos de otro MS (sin comodín)", List.of(ADMIN), "course.grades", Permission.READ, false),
            scenario("ADMIN NO accede a un recurso de Auth no concedido", List.of(ADMIN), AuthResourceId.PERMISSIONS_EFFECTIVE, Permission.READ, false),

            // --- DOCENTE: sólo lectura sobre permissions.effective ---
            scenario("DOCENTE lee permissions.effective", List.of(DOCENTE), AuthResourceId.PERMISSIONS_EFFECTIVE, Permission.READ, true),
            scenario("DOCENTE NO escribe permissions.effective", List.of(DOCENTE), AuthResourceId.PERMISSIONS_EFFECTIVE, Permission.WRITE, false),
            scenario("DOCENTE NO lee auth.users", List.of(DOCENTE), AuthResourceId.USERS, Permission.READ, false),

            // --- Sin roles ⇒ sin grants ⇒ denegado ---
            scenario("usuario sin roles es denegado", List.of(), AuthResourceId.USERS, Permission.READ, false),

            // --- Multi-rol: la unión (OR) de roles acumula permisos ---
            scenario("DOCENTE+ADMIN combina lectura efectiva y escritura de usuarios (A)",
                List.of(DOCENTE, ADMIN), AuthResourceId.USERS, Permission.WRITE, true),
            scenario("DOCENTE+ADMIN combina lectura efectiva y escritura de usuarios (B)",
                List.of(DOCENTE, ADMIN), AuthResourceId.PERMISSIONS_EFFECTIVE, Permission.READ, true),

            // --- Grants parciales sobre el MISMO recurso se OR-ean a un permiso mayor ---
            scenario("READER+WRITER ⇒ lectura sobre auth.users", List.of(READER, WRITER), AuthResourceId.USERS, Permission.READ, true),
            scenario("READER+WRITER ⇒ escritura sobre auth.users", List.of(READER, WRITER), AuthResourceId.USERS, Permission.WRITE, true),
            scenario("READER+WRITER (rw) NO concede ejecución", List.of(READER, WRITER), AuthResourceId.USERS, Permission.EXECUTE, false),
            scenario("READER solo NO escribe auth.users", List.of(READER), AuthResourceId.USERS, Permission.WRITE, false)
        );
    }

    private static Arguments scenario(String desc, List<UUID> roleIds, String resourceKey,
                                      Permission required, boolean expected) {
        return Arguments.of(desc, roleIds, resourceKey, required, expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matrix")
    void accessMatrix(String desc, List<UUID> roleIds, String resourceKey,
                      Permission required, boolean expected) {
        boolean actual = service.hasPermission(roleIds, resourceKey, required.bit);
        assertEquals(expected, actual, desc);
    }

    @ParameterizedTest(name = "consistencia text/plain vs json — {0}")
    @MethodSource("matrix")
    void plainAndJsonDecisionsAgree(String desc, List<UUID> roleIds, String resourceKey,
                                    Permission required, boolean expected) {
        // El endpoint text/plain usa hasPermission; el json usa effectiveFlags & bit.
        // Ambos caminos deben coincidir (misma fuente de verdad).
        boolean viaHasPermission = service.hasPermission(roleIds, resourceKey, required.bit);
        short effective = service.effectiveFlags(roleIds, resourceKey);
        boolean viaEffectiveFlags = (effective & required.bit) == required.bit;
        assertEquals(viaHasPermission, viaEffectiveFlags, desc);
        assertEquals(expected, viaHasPermission, desc);
    }
}
