package cl.duocuc.edutrack.ms.infrastructure.exception;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Envelope JSON único para errores. Lo emite {@code GlobalExceptionMappers}.
 *
 * <p>Campos opcionales ({@code code}, {@code metadata}, {@code trace}) se
 * omiten cuando son {@code null}/vacíos vía {@link JsonInclude#NON_EMPTY},
 * por lo que el envelope se mantiene mínimo cuando no hay datos.</p>
 *
 * @param timestamp instante del error en el servidor (UTC, ISO-8601)
 * @param status    código HTTP — sí, también va en el body, para que clientes
 *                  que pierden el status en logs/relay igual lo tengan a mano
 * @param error     reason phrase del status (e.g. {@code "Not Found"})
 * @param code      código de dominio opcional (e.g. {@code "AUTH.USER.EMAIL_EXISTS"})
 * @param message   mensaje legible para humanos
 * @param path      path del request
 * @param metadata  contexto adicional estructurado (opcional)
 * @param trace     stack trace resumida, solo cuando
 *                  {@code edutrack.errors.expose-stacktrace=true}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    @JsonView(Views.Base.class) Instant timestamp,
    @JsonView(Views.Base.class) int status,
    @JsonView(Views.Base.class) String error,
    @JsonView(Views.Base.class) String code,
    @JsonView(Views.Base.class) String message,
    @JsonView(Views.Base.class) String path,
    @JsonView(Views.Base.class) Map<String, Object> metadata,
    @JsonView(Views.Base.class) List<String> trace
) {}
