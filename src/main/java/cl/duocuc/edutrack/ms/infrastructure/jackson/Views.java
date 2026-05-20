package cl.duocuc.edutrack.ms.infrastructure.jackson;

/**
 * Jerarquía estándar de vistas Jackson ({@code @JsonView}) compartida por
 * todos los microservicios. Modela la granularidad de campos por endpoint sin
 * necesidad de crear DTOs adicionales (convención de "un Request, un Response"
 * por entidad).
 *
 * <p>Cada servicio puede extender esta jerarquía con vistas propias del dominio
 * (p. ej. en Auth: {@code AuthViews.Login}, {@code AuthViews.Refresh}). Lo que
 * sí es transversal — y vive aquí — es el núcleo {@code Base/Extra/Detailed/...}.</p>
 *
 * <p>{@code infrastructure.jackson.JacksonCustomConfig} configura {@link Base}
 * como vista por defecto y desactiva {@code DEFAULT_VIEW_INCLUSION}: solo se
 * serializan/deserializan campos que declaren explícitamente una vista
 * compatible. Como consecuencia, no es necesario anotar
 * {@code @JsonView(Views.Base.class)} explícitamente en endpoints/parámetros.</p>
 */
public interface Views {

    interface Base {}

    interface Extra {}

    interface Detailed extends Base {}

    interface Create extends Base {}

    interface List extends Base, Extra {}

    interface Patch extends Base, Extra {}

    interface Update extends Base {}

    interface Admin extends Base, Extra {}

    interface Internal {}
}
