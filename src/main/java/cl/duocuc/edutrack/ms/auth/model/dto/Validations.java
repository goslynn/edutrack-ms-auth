package cl.duocuc.edutrack.ms.auth.model.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Grupos de Bean Validation para modelar validaciones condicionales por endpoint
 * sobre un único record Request (mismo criterio de granularidad que las vistas
 * {@code @JsonView}).
 *
 * <p><b>Convención:</b></p>
 * <ul>
 *   <li>Las restricciones de <i>formato</i> ({@code @Email}, {@code @Size},
 *       {@code @Min}, {@code @Max}) van en el grupo {@code Default}: siempre se
 *       evalúan y son null-safe (pasan cuando el campo no viaja en esa vista).</li>
 *   <li>Las restricciones de <i>presencia</i> ({@code @NotBlank}, {@code @NotNull})
 *       se anotan con {@code groups = Validations.OnXxx.class} y solo se evalúan en
 *       el endpoint correspondiente.</li>
 * </ul>
 *
 * <p>El recurso JAX-RS dispara el grupo combinado anotando el parámetro de body con
 * {@code @Valid @ConvertGroup(from = Default.class, to = Validations.Xxx.class)}.
 * Cada {@code Xxx} es un {@code @GroupSequence} que ejecuta primero {@code Default}
 * (formato) y luego el grupo de presencia del endpoint. Los endpoints sin presencia
 * obligatoria (p. ej. {@code PUT}) usan {@code @Valid} a secas (solo {@code Default}).</p>
 */
public interface Validations {

    /** Marcadores de presencia obligatoria, activados por endpoint. */
    interface OnCreate {}
    interface OnLogin {}
    interface OnRefresh {}

    /** POST de creación: formato + presencia obligatoria. */
    @GroupSequence({ Default.class, OnCreate.class })
    interface Create {}

    /** POST /auth/login: formato + presencia de credenciales. */
    @GroupSequence({ Default.class, OnLogin.class })
    interface Login {}

    /** POST /auth/refresh: formato + presencia del refresh token. */
    @GroupSequence({ Default.class, OnRefresh.class })
    interface Refresh {}
}
