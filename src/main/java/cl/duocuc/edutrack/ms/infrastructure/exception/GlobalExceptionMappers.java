package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Handler global de excepciones. Convierte cualquier excepción que escape de
 * un recurso en un {@link ErrorResponse} con forma estable
 * ({@code timestamp}, {@code status}, {@code code}, {@code message},
 * {@code path}, {@code metadata}, opcional {@code trace}).
 *
 * <p>Reglas por tipo (más específico primero, Quarkus REST resuelve por
 * jerarquía):</p>
 * <ul>
 *   <li>{@link DomainException} ⇒ usa su status, code, message, metadata.</li>
 *   <li>{@link ConstraintViolationException} ⇒ {@code 400}, code
 *       {@code VALIDATION.CONSTRAINT}, metadata {@code violations[]} con
 *       {@code path}/{@code message} por violación.</li>
 *   <li>{@link WebApplicationException} ⇒ status de la response que carga,
 *       sin code (legacy / excepciones de framework).</li>
 *   <li>{@link Throwable} ⇒ {@code 500} con code {@code INTERNAL.UNEXPECTED}.
 *       Loguea en {@code ERROR}; los demás casos se loguean en {@code DEBUG}.</li>
 * </ul>
 *
 * <p>El stack trace solo se incluye en el body cuando
 * {@code edutrack.errors.expose-stacktrace=true}. Default {@code false}:
 * recortar la traza en prod evita filtrar paquetes/clases internas y reduce
 * el tamaño del body; activarlo en dev/staging mejora el debuggeo
 * exponencialmente. El costo de cómputo es mínimo (la traza ya existe en el
 * objeto excepción); el costo real es el ancho de banda y la fuga de info.</p>
 */
@ApplicationScoped
public class GlobalExceptionMappers {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMappers.class);

    /** Máximo de frames de stack trace expuestos (cuando el flag está activo). */
    private static final int MAX_TRACE_FRAMES = 25;

    @ConfigProperty(name = "edutrack.errors.expose-stacktrace", defaultValue = "false")
    boolean exposeStacktrace;

    @ServerExceptionMapper
    public Response mapDomain(DomainException ex, UriInfo uri) {
        LOG.debugf(ex, "DomainException %s -> %d", ex.code(), ex.status());
        return build(ex.status(), ex.code(), ex.getMessage(), uri, ex.metadata(), ex);
    }

    @ServerExceptionMapper
    public Response mapConstraintViolation(ConstraintViolationException ex, UriInfo uri) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
            .map(this::renderViolation)
            .toList();
        Map<String, Object> meta = Map.of("violations", violations);
        LOG.debugf("ConstraintViolation %s", violations);
        return build(400, "VALIDATION.CONSTRAINT",
            "Request validation failed", uri, meta, ex);
    }

    @ServerExceptionMapper
    public Response mapWebApplication(WebApplicationException ex, UriInfo uri) {
        int status = ex.getResponse() != null
            ? ex.getResponse().getStatus()
            : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        // No exponemos message del WAE genérico (suele ser ruido tipo "HTTP 404 Not Found");
        // si el caller quiere un mensaje útil, debería lanzar DomainException.
        String msg = status >= 500 ? "Internal server error" : reasonPhrase(status);
        LOG.debugf(ex, "WebApplicationException -> %d", status);
        return build(status, null, msg, uri, Map.of(), ex);
    }

    @ServerExceptionMapper
    public Response mapUnhandled(Throwable ex, UriInfo uri) {
        LOG.errorf(ex, "Unhandled exception on %s", path(uri));
        return build(500, "INTERNAL.UNEXPECTED",
            "Internal server error", uri, Map.of(), ex);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Response build(int status, String code, String message, UriInfo uri,
                           Map<String, Object> metadata, Throwable ex) {
        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            status,
            reasonPhrase(status),
            code,
            message,
            path(uri),
            metadata == null || metadata.isEmpty() ? null : new LinkedHashMap<>(metadata),
            exposeStacktrace ? renderTrace(ex) : null
        );
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private Map<String, String> renderViolation(ConstraintViolation<?> v) {
        return Map.of(
            "path", v.getPropertyPath().toString(),
            "message", v.getMessage()
        );
    }

    private List<String> renderTrace(Throwable ex) {
        if (ex == null) return null;
        return Stream.of(ex.getStackTrace())
            .limit(MAX_TRACE_FRAMES)
            .map(StackTraceElement::toString)
            .toList();
    }

    private static String path(UriInfo uri) {
        return uri == null ? null : "/" + uri.getPath();
    }

    private static String reasonPhrase(int status) {
        Response.Status s = Response.Status.fromStatusCode(status);
        return s == null ? "HTTP " + status : s.getReasonPhrase();
    }
}
