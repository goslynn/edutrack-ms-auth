package cl.duocuc.edutrack.ms.infrastructure.context;

/**
 * Única fuente de verdad de los nombres de las cabeceras internas que el API
 * Gateway propaga tras validar el JWT. Ningún otro punto del servicio debe
 * referenciar estos strings: el "significado" de cada header se expone como
 * campos tipados en {@link RequestHeaders}.
 */
public enum InternalHeader {

    /** UUID del usuario autenticado (claim {@code sub} del JWT). */
    USER_ID("X-User-Id"),

    /** UUIDs de los roles del usuario, separados por coma. */
    USER_ROLES("X-User-Roles");

    public final String wire;

    InternalHeader(String wire) {
        this.wire = wire;
    }
}
