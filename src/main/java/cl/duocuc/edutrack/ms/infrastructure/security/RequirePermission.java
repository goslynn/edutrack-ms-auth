package cl.duocuc.edutrack.ms.infrastructure.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Exige que el llamante tenga, sobre {@link #resource()}, al menos el bit de
 * permiso {@link #value()}. {@link RequirePermissionFilter} resuelve los flags
 * efectivos a partir del header {@code X-User-Roles} (UUIDs de rol propagados
 * por el API Gateway) y aborta con {@code 403} si no alcanzan el mínimo.
 *
 * <p>Sustituye al antiguo {@code RoleGuard}: ya no se valida contra nombres de
 * rol (azúcar visual) sino contra los permisos Unix-style del recurso.</p>
 */
@NameBinding
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface RequirePermission {

    /** Recurso de Auth sobre el que se evalúa el permiso. */
    AuthResourceId resource();

    /** Bit mínimo requerido (READ / WRITE / EXECUTE). */
    Permission value();

    /**
     * Nombre de un path-param que, si coincide con el header {@code X-User-Id},
     * permite el acceso sin chequear permisos (acceso a recursos propios).
     * Vacío = sin excepción de "self".
     */
    String selfParam() default "";
}
