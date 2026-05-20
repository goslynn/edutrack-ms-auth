package cl.duocuc.edutrack.ms.auth.security;

import java.util.UUID;

/**
 * Catálogo de recursos que el propio Auth Service protege con permisos
 * Unix-style. Auth registra los suyos igual que cualquier otro MS; estos
 * valores son fijos y deben coincidir con los sembrados en la migración
 * Flyway {@code V4__seed_role_permissions.sql}.
 *
 * <p>{@code PERMISSIONS} (administración de {@code role_permissions}) y
 * {@code PERMISSIONS_EFFECTIVE} (consulta de flags efectivos para otros MS) son
 * recursos distintos a propósito: la consulta efectiva es legible por DOCENTE
 * mientras que la administración no.</p>
 *
 * <p>Las constantes anidadas en {@link Uuid} reproducen los mismos UUIDs como
 * {@link String} de tiempo de compilación, para usarlas como valor de
 * anotaciones (p. ej. {@code @RequirePermission(resource = AuthResourceId.Uuid.USERS, ...)}).
 * El wildcard global vive en {@code infrastructure.security.ResourceIds}.</p>
 */
public enum AuthResourceId {
    USERS(Uuid.USERS),
    ROLES(Uuid.ROLES),
    PERMISSIONS(Uuid.PERMISSIONS),
    PERMISSIONS_EFFECTIVE(Uuid.PERMISSIONS_EFFECTIVE),
    USER_ROLES(Uuid.USER_ROLES);

    public final UUID uuid;

    AuthResourceId(String uuid) {
        this.uuid = UUID.fromString(uuid);
    }

    /** UUIDs como {@link String} (aptos para anotaciones). */
    public interface Uuid {
        String USERS                = "00000000-0000-0000-0000-00000000a001";
        String ROLES                = "00000000-0000-0000-0000-00000000a002";
        String PERMISSIONS          = "00000000-0000-0000-0000-00000000a003";
        String PERMISSIONS_EFFECTIVE = "00000000-0000-0000-0000-00000000a004";
        String USER_ROLES           = "00000000-0000-0000-0000-00000000a005";
    }
}
