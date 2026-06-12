package cl.duocuc.edutrack.ms.auth.security;

/**
 * Catálogo de recursos que el propio Auth Service protege con permisos
 * Unix-style. Auth registra los suyos igual que cualquier otro MS; estas claves
 * son fijas y deben coincidir con las sembradas en la migración Flyway
 * {@code V2__seed.sql}.
 *
 * <p>Una <em>resource key</em> es un identificador estable y legible
 * ({@code "<servicio>.<recurso>"}); es opaca para el modelo de permisos (solo
 * se compara por igualdad) y constituye el contrato cross-servicio: el mismo
 * string nombra el recurso en ambos lados de un grant, sin UUIDs que coordinar.</p>
 *
 * <p>{@code PERMISSIONS} (administración de {@code role_permissions}) y
 * {@code PERMISSIONS_EFFECTIVE} (consulta de flags efectivos para otros MS) son
 * recursos distintos a propósito: la consulta efectiva es legible por DOCENTE
 * mientras que la administración no.</p>
 *
 * <p>Las constantes anidadas en reproducen las mismas claves como
 * {@link String} de tiempo de compilación, para usarlas como valor de
 * anotaciones (p. ej. {@code @RequirePermission(resource = AuthResourceId.Key.USERS, ...)}).
 * El wildcard global vive en {@code infrastructure.security.ResourceIds}.</p>
 */
// TODO(resource-catalog): exponer este catálogo vía GET /auth/meta/resources para que el
//  frontend descubra las resource keys asignables (no hay tabla `resources`; la fuente de
//  verdad es este enum). Es la implementación concreta para Auth de la decisión descentralizada
//  documentada en infrastructure.security.ResourceIds. Replicar el patrón en cada MS nuevo.
public interface AuthResourceId {
    String USERS                 = "auth.users";
    String ROLES                 = "auth.roles";
    String PERMISSIONS           = "auth.permissions";
    String PERMISSIONS_EFFECTIVE = "auth.permissions.effective";
    String USER_ROLES            = "auth.user-roles";

}
