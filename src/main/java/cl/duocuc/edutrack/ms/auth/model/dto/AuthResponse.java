package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;

public record AuthResponse(
    @JsonView(Views.Base.class) String accessToken,
    @JsonView(Views.Base.class) String refreshToken,
    @JsonView(Views.Base.class) String tokenType,
    @JsonView(Views.Base.class) long expiresIn
) {

    /**
     * {@code AuthResponse} no respalda una entidad: nace del resultado de la
     * emisión de tokens. El contrato {@code *Response sabe construirse} se
     * cumple igualmente: el call site no compone el DTO a mano. El
     * {@code tokenType} {@code "Bearer"} es invariante del protocolo y vive
     * aquí en lugar de duplicarse en cada caller.
     */
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
