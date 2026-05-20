package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;

/**
 * Vistas específicas del dominio de autenticación. Extienden la jerarquía
 * estándar {@link Views} con los marcadores que solo aplican aquí.
 */
public interface AuthViews {

    /** Body del POST {@code /auth/login}. */
    interface Login extends Views.Base {}

    /** Body del POST {@code /auth/refresh}. */
    interface Refresh extends Views.Base {}
}
