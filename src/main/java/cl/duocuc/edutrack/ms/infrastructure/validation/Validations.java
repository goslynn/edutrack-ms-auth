package cl.duocuc.edutrack.ms.infrastructure.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Grupos de Bean Validation transversales para modelar validaciones
 * condicionales por endpoint sobre un único record Request (mismo criterio de
 * granularidad que las vistas {@code @JsonView}). Compartido por todos los
 * microservicios.
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
 * <p>El recurso JAX-RS dispara el grupo combinado anotando el parámetro de body
 * con {@code @Valid @ConvertGroup(from = Default.class, to = Validations.Xxx.class)}.
 * Cada {@code Xxx} es un {@code @GroupSequence} que ejecuta primero {@code Default}
 * (formato) y luego el grupo de presencia del endpoint. Los endpoints sin
 * presencia obligatoria (p. ej. {@code PUT}) usan {@code @Valid} a secas (solo
 * {@code Default}).</p>
 *
 * <p>Esta interfaz cubre el grupo {@code OnCreate} y su {@code @GroupSequence}
 * porque son transversales (cualquier MS expone un POST de creación). Los
 * marcadores específicos de cada dominio se agregan en una interfaz hermana
 * dentro del propio MS (p. ej. {@code AuthValidations.OnLogin} en auth).</p>
 */
public interface Validations {

    /** Marcador de presencia obligatoria para POST de creación. */
    interface OnCreate {}

    /** POST de creación: formato + presencia obligatoria. */
    @GroupSequence({ Default.class, OnCreate.class })
    interface Create {}
}
