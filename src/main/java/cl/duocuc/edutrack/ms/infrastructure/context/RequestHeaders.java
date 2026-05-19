package cl.duocuc.edutrack.ms.infrastructure.context;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vista request-scoped, ya interpretada y validada, de las cabeceras internas
 * que el Gateway propaga. Se computa <b>una sola vez por request</b>
 * ({@link RequestContext}) y se inyecta por CDI:
 *
 * <pre>{@code @Inject RequestHeaders headers;}</pre>
 *
 * <p>Abstrae el "significado" de cada cabecera: los consumidores leen
 * {@link #userId()} / {@link #roleIds()} y nunca el string {@code "X-..."}.
 * Un valor ausente (header no enviado, o malformado en modo
 * {@link HeaderValidationMode#WARN}) se expresa como {@link Optional#empty()} /
 * lista vacía — nunca {@code null}.</p>
 *
 * @param userId  UUID del usuario autenticado, si el Gateway lo propagó
 * @param roleIds UUIDs de rol del usuario; lista inmutable, vacía si no hay
 */
public record RequestHeaders(Optional<UUID> userId, List<UUID> roleIds) {

    /** ¿El Gateway propagó una identidad de usuario? */
    public boolean hasIdentity() {
        return userId.isPresent();
    }

    /**
     * Identidad propagada o {@code 401}. Su ausencia es un fallo de
     * autenticación/identidad (no de validación de datos): sin identidad no hay
     * a quién atribuir la operación.
     */
    public UUID requireUserId() {
        return userId.orElseThrow(
            () -> new WebApplicationException(Response.Status.UNAUTHORIZED));
    }
}
