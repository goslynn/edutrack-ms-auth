package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Par de tokens emitido tras login o refresh.")
public record AuthResponse(
    @Schema(description = "JWT RS256 de acceso; va en el header Authorization: Bearer") String accessToken,
    @Schema(description = "Token opaco para renovar el access token via POST /auth/refresh") String refreshToken,
    @Schema(description = "Tipo de token", examples = "Bearer") String tokenType,
    @Schema(description = "Vigencia del access token en segundos", examples = "900") long expiresIn
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
