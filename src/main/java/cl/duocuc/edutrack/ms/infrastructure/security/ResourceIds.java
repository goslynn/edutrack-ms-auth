package cl.duocuc.edutrack.ms.infrastructure.security;

import java.util.UUID;

/**
 * Constantes transversales del modelo de permisos. Compartidas por todos los
 * microservicios: cada MS define adicionalmente su propio catálogo de
 * {@code resource_uuid}s opacos, pero estos valores son comunes.
 *
 * <p>{@link #ALL} es un recurso comodín ("todos los recursos, presentes y
 * futuros"): un grant de flags sobre {@code ALL} aplica a cualquier recurso.
 * No se usa para anotar endpoints — solo se consulta al hacer el OR con el
 * recurso concreto.</p>
 */
public final class ResourceIds {

    /** UUID comodín como {@link String} (apto para anotaciones). */
    public static final String ALL_UUID = "00000000-0000-0000-0000-000000000000";

    /** UUID comodín ya tipado. */
    public static final UUID ALL = UUID.fromString(ALL_UUID);

    private ResourceIds() {}
}
