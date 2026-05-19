package cl.duocuc.edutrack.ms.infrastructure.security;

import java.util.UUID;

/**
 * UUIDs opacos de los recursos que el propio Auth Service protege con permisos
 * Unix-style. Auth registra los suyos igual que cualquier otro MS; estos valores
 * son fijos y deben coincidir con los sembrados en la migración Flyway
 * {@code V4__seed_role_permissions.sql}.
 *
 * <p>{@code PERMISSIONS} (administración de {@code role_permissions}) y
 * {@code PERMISSIONS_EFFECTIVE} (consulta de flags efectivos para otros MS) son
 * recursos distintos a propósito: la consulta efectiva es legible por DOCENTE
 * mientras que la administración no.</p>
 *
 * <p>{@link #ALL} es un recurso comodín ("todos los recursos, presentes y
 * futuros"): un grant de flags sobre {@code ALL} aplica a cualquier recurso.
 * No se usa para anotar endpoints — solo lo consulta el filtro al hacer el OR
 * con el recurso concreto. Sembrado a SUPERUSER en {@code V5} para que sea
 * omnipotente sin filas por recurso concreto.</p>
 */
public enum AuthResourceId {
    USERS("00000000-0000-0000-0000-00000000a001"),
    ROLES("00000000-0000-0000-0000-00000000a002"),
    PERMISSIONS("00000000-0000-0000-0000-00000000a003"),
    PERMISSIONS_EFFECTIVE("00000000-0000-0000-0000-00000000a004"),
    USER_ROLES("00000000-0000-0000-0000-00000000a005"),
    ALL("00000000-0000-0000-0000-000000000000");

    public final UUID uuid;

    AuthResourceId(String uuid) {
        this.uuid = UUID.fromString(uuid);
    }
}
