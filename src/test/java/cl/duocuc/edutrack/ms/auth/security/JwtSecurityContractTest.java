package cl.duocuc.edutrack.ms.auth.security;

import cl.duocuc.edutrack.ms.auth.support.JwtTestSupport;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato de seguridad del JWT RS256 (requisito 1), probado de forma atómica y
 * agnóstica: sin arrancar Quarkus, sin DB, sin Gateway. Firma tokens con la
 * misma forma que emite {@code TokenService} (issuer {@code edutrack-auth},
 * {@code sub} = UUID de usuario, claim {@code roles}, {@code exp}) y los valida
 * con el mismo verificador SmallRye MP-JWT que aplicaría el Gateway.
 *
 * <p>Cubre: token válido y funcional (1.1), rechazo de token expirado (1.2) y la
 * integridad de la firma —forja con otra llave, manipulación del payload, issuer
 * incorrecto— que sostiene todo el modelo de confianza stateless.</p>
 */
class JwtSecurityContractTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final List<String> ROLES = List.of(
        "22222222-2222-2222-2222-222222222222",
        "33333333-3333-3333-3333-333333333333");

    /** Token tal como lo construye {@code TokenService.buildAccessToken}, firmado con la llave dada. */
    private String issueAccessToken(java.security.PrivateKey signingKey, Instant expiresAt) {
        return Jwt.issuer(JwtTestSupport.ISSUER)
            .subject(USER_ID.toString())
            .claim("roles", ROLES)
            .expiresAt(expiresAt)
            .sign(signingKey);
    }

    @Test
    @DisplayName("1.1 — un JWT válido se verifica y transporta la identidad y roles del usuario")
    void validToken_isFunctionalAndCarriesIdentity() throws ParseException {
        String token = issueAccessToken(
            JwtTestSupport.testPrivateKey(),
            Instant.now().plus(15, ChronoUnit.MINUTES));

        JsonWebToken jwt = JwtTestSupport.verify(token);

        assertEquals(USER_ID.toString(), jwt.getSubject(), "sub debe ser el UUID del usuario");
        assertEquals(JwtTestSupport.ISSUER, jwt.getIssuer());
        assertNotNull(jwt.getExpirationTime());
        assertTrue(jwt.getExpirationTime() > Instant.now().getEpochSecond(),
            "el token aún no debe estar expirado");

        List<String> roles = JwtTestSupport.stringListClaim(jwt, "roles");
        assertEquals(ROLES.size(), roles.size());
        assertTrue(roles.containsAll(ROLES), "el claim roles debe contener los UUIDs de rol del usuario");
    }

    @Test
    @DisplayName("1.2 — un JWT expirado es rechazado por el verificador")
    void expiredToken_isRejected() {
        String expired = issueAccessToken(
            JwtTestSupport.testPrivateKey(),
            Instant.now().minus(1, ChronoUnit.HOURS));

        assertThrows(ParseException.class, () -> JwtTestSupport.verify(expired),
            "un token con exp en el pasado no debe verificar");
    }

    @Test
    @DisplayName("Integridad — un token firmado con otra llave (forja) es rechazado")
    void tokenSignedWithForeignKey_isRejected() {
        KeyPair attacker = JwtTestSupport.generateRsaKeyPair();
        String forged = issueAccessToken(
            attacker.getPrivate(),
            Instant.now().plus(15, ChronoUnit.MINUTES));

        // Firmado con la privada del atacante, pero verificado contra la pública legítima.
        assertThrows(ParseException.class, () -> JwtTestSupport.verify(forged),
            "una firma RS256 que no corresponde a la llave de Auth no debe verificar");
    }

    @Test
    @DisplayName("Integridad — un token con el payload manipulado es rechazado")
    void tamperedToken_isRejected() {
        String token = issueAccessToken(
            JwtTestSupport.testPrivateKey(),
            Instant.now().plus(15, ChronoUnit.MINUTES));

        // Altera un carácter del segmento de payload: la firma deja de cuadrar.
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[0] = (payload[0] == 'A') ? 'B' : 'A';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThrows(ParseException.class, () -> JwtTestSupport.verify(tampered),
            "manipular el payload invalida la firma");
    }

    @Test
    @DisplayName("Integridad — un token con issuer incorrecto es rechazado")
    void tokenWithWrongIssuer_isRejected() {
        String token = Jwt.issuer("evil-issuer")
            .subject(USER_ID.toString())
            .claim("roles", ROLES)
            .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
            .sign(JwtTestSupport.testPrivateKey());

        // Firma válida, pero el issuer no es edutrack-auth.
        assertThrows(ParseException.class, () -> JwtTestSupport.verify(token),
            "el verificador debe exigir el issuer esperado");
    }
}
