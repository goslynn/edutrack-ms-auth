package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excepción base para errores de dominio del MS. A diferencia de
 * {@link jakarta.ws.rs.WebApplicationException} (que solo lleva status), esta
 * carga además:
 *
 * <ul>
 *   <li>{@link #code()} — código estable de la condición ({@code AUTH.USER.EMAIL_EXISTS}),
 *       pensado para que clientes/UI hagan switch sobre él sin parsear el mensaje;</li>
 *   <li>{@link #metadata()} — contexto adicional estructurado
 *       ({@code Map<String,Object>}) que el handler global incluye en la
 *       respuesta;</li>
 *   <li>{@link #status()} — código HTTP que se debe responder.</li>
 * </ul>
 *
 * <p>Patrón de uso (ver {@code ConflictException}, {@code NotFoundException},
 * {@code ForbiddenException}):</p>
 *
 * <pre>{@code
 * throw new ConflictException("AUTH.USER.EMAIL_EXISTS", "Email already in use")
 *     .with("email", email);
 * }</pre>
 *
 * <p>El handler global ({@code GlobalExceptionMappers}) la convierte en el
 * envelope {@code ErrorResponse} con {@code status}, {@code code},
 * {@code message}, {@code metadata} y {@code timestamp}.</p>
 */
public class DomainException extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public DomainException(Response.Status status, String code, String message) {
        this(status.getStatusCode(), code, message);
    }

    public DomainException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    /** Agrega un par a la metadata. Encadenable en el {@code throw}. */
    public DomainException with(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /** Carga toda la metadata desde un map (no reemplaza, hace merge). */
    public DomainException withAll(Map<String, Object> entries) {
        if (entries != null) metadata.putAll(entries);
        return this;
    }

    public int status() { return status; }
    public String code() { return code; }
    public Map<String, Object> metadata() { return Collections.unmodifiableMap(metadata); }
}
