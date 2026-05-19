package cl.duocuc.edutrack.ms.infrastructure.context;

/**
 * Política de fallo cuando una cabecera interna viene presente pero malformada
 * (p. ej. {@code X-User-Id} que no es un UUID). La <b>ausencia</b> de un header
 * nunca es un fallo de validación: se interpreta como valor vacío.
 *
 * <p>Configurable vía {@code edutrack.headers.validation.mode}.</p>
 */
public enum HeaderValidationMode {

    /** Header malformado ⇒ se aborta el request con {@code 400 Bad Request}. */
    EAGER,

    /**
     * Header malformado ⇒ se loguea un {@code WARN} y el valor se trata como
     * ausente; el request continúa. Útil tras el Gateway de confianza para no
     * tumbar tráfico por un header mal formado aguas arriba.
     */
    WARN
}
