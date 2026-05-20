package cl.duocuc.edutrack.ms.infrastructure.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Exige que el llamante tenga, sobre el recurso identificado por {@link #resource()},
 * al menos el bit de permiso {@link #value()}. {@link RequirePermissionFilter}
 * resuelve los flags efectivos a partir de la identidad propagada por el API
 * Gateway y aborta con {@code 403} si no alcanzan el mínimo.
 *
 * <p>El recurso se referencia por su UUID en formato {@link String} (constante
 * de compilación). Cada microservicio define sus propias constantes — el
 * paquete {@code infrastructure.security} no las conoce.</p>
 */
@NameBinding
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface RequirePermission {

    /** UUID (texto) del recurso sobre el que se evalúa el permiso. */
    String resource();

    /** Bit mínimo requerido (READ / WRITE / EXECUTE). */
    Permission value();

    /**
     * Nombre de un path-param que, si coincide con la identidad propagada
     * por el Gateway, permite el acceso sin chequear permisos (acceso a
     * recursos propios). Vacío = sin excepción de "self".
     */
    String selfParam() default "";
}
