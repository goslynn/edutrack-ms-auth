package cl.duocuc.edutrack.ms.auth.model.dto;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Grupos de Bean Validation específicos del dominio de autenticación. Conviven
 * con la interfaz transversal
 * {@link cl.duocuc.edutrack.ms.infrastructure.validation.Validations}: lo
 * común ({@code OnCreate}, secuencia {@code Create}) vive allá; aquí solo lo
 * propio del MS.
 */
public interface AuthValidations {

    /** Marcadores de presencia obligatoria, activados por endpoint. */
    interface OnLogin {}
    interface OnRefresh {}

    /** POST {@code /auth/login}: formato + presencia de credenciales. */
    @GroupSequence({ Default.class, OnLogin.class })
    interface Login {}

    /** POST {@code /auth/refresh}: formato + presencia del refresh token. */
    @GroupSequence({ Default.class, OnRefresh.class })
    interface Refresh {}
}
