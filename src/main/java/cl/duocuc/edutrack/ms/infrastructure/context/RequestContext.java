package cl.duocuc.edutrack.ms.infrastructure.context;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Único intérprete de las cabeceras internas que el API Gateway propaga.
 *
 * <p>Bean <b>request-scoped</b> y proxyable: las cabeceras se interpretan y
 * validan <b>una sola vez por request HTTP</b> ({@link PostConstruct}) y el
 * resultado se expone como el record inmutable {@link #headers()}. Ningún
 * endpoint ni filtro debe volver a leer headers {@code "X-..."} por su cuenta:
 * inyectan este contexto.</p>
 *
 * <pre>{@code
 * @Inject RequestContext ctx;
 * ...
 * UUID uid = ctx.headers().requireUserId();
 * List<UUID> roles = ctx.headers().roleIds();
 * }</pre>
 *
 * <p>Reglas de interpretación:</p>
 * <ol>
 *   <li>Header presente y bien formado ⇒ valor tipado.</li>
 *   <li>Header ausente ⇒ valor vacío ({@link Optional#empty()} / lista vacía).
 *       <b>No</b> es un fallo de validación.</li>
 *   <li>Header presente pero malformado ⇒ según
 *       {@code edutrack.headers.validation.mode}:
 *       {@link HeaderValidationMode#EAGER} aborta con {@code 400};
 *       {@link HeaderValidationMode#WARN} loguea y trata el valor como ausente.</li>
 * </ol>
 */
@RequestScoped
public class RequestContext {

    private static final Logger LOG = Logger.getLogger(RequestContext.class);

    @Inject
    RoutingContext routingContext;

    @ConfigProperty(name = "edutrack.headers.validation.mode", defaultValue = "EAGER")
    HeaderValidationMode mode;

    private RequestHeaders headers;

    @PostConstruct
    void interpret() {
        headers = new RequestHeaders(parseUserId(), parseRoleIds());
    }

    /** Vista inmutable, ya interpretada y validada, de las cabeceras del request. */
    public RequestHeaders headers() {
        return headers;
    }

    private Optional<UUID> parseUserId() {
        String raw = header(InternalHeader.USER_ID);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return onMalformed(InternalHeader.USER_ID, raw, Optional.empty());
        }
    }

    private List<UUID> parseRoleIds() {
        String raw = header(InternalHeader.USER_ROLES);
        if (raw == null) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(t));
            } catch (IllegalArgumentException e) {
                return onMalformed(InternalHeader.USER_ROLES, raw, List.of());
            }
        }
        return List.copyOf(ids);
    }

    /** Header con trim; {@code null} si está ausente o en blanco. */
    private String header(InternalHeader h) {
        String v = routingContext.request().getHeader(h.wire);
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Política de header malformado. En {@link HeaderValidationMode#EAGER}
     * aborta el request con {@code 400}; en {@link HeaderValidationMode#WARN}
     * loguea y devuelve el valor vacío {@code fallback}.
     */
    private <T> T onMalformed(InternalHeader h, String raw, T fallback) {
        if (mode == HeaderValidationMode.EAGER) {
            throw new WebApplicationException(
                "Cabecera interna malformada: " + h.wire,
                Response.Status.BAD_REQUEST);
        }
        LOG.warnf("Cabecera interna malformada %s=%s — se ignora (modo WARN)", h.wire, raw);
        return fallback;
    }
}
