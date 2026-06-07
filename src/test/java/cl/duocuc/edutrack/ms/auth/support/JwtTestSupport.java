package cl.duocuc.edutrack.ms.auth.support;

import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.json.JsonArray;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utilidades de test para el contrato JWT RS256, sin arrancar Quarkus.
 *
 * <p>Expone (a) la verificación de un token tal como la haría el verificador
 * MP-JWT del Gateway —firma RS256 + issuer + expiración— vía el mismo
 * {@link JWTParser} de SmallRye que usa el runtime, y (b) acceso a la llave de
 * test commiteada en {@code src/test/resources} (la misma que firma los tokens
 * en los tests de {@code TokenService}, vía {@code smallrye.jwt.sign.key.location}).</p>
 */
public final class JwtTestSupport {

    /** Issuer que emite Auth Service ({@code TokenService.JWT_ISSUER}). */
    public static final String ISSUER = "edutrack-auth";

    private JwtTestSupport() {
    }

    /**
     * Verifica un token como lo haría el consumidor (Gateway): valida la firma
     * RS256 contra {@code key}, exige el {@code issuer} y rechaza tokens
     * expirados (sin periodo de gracia). Lanza {@link ParseException} si algo
     * no cuadra.
     */
    public static JsonWebToken verify(String token, PublicKey key, String issuer) throws ParseException {
        JWTAuthContextInfo ctx = new JWTAuthContextInfo(key, issuer);
        ctx.setExpGracePeriodSecs(0);
        JWTParser parser = new DefaultJWTParser(ctx);
        return parser.parse(token);
    }

    /** Verifica contra la llave pública de test y el issuer de Auth. */
    public static JsonWebToken verify(String token) throws ParseException {
        return verify(token, testPublicKey(), ISSUER);
    }

    /**
     * Extrae un claim de lista de strings de forma robusta. Tras el round-trip
     * por JWT, un claim de lista vuelve como {@link JsonArray} de
     * {@link JsonString}; este helper lo normaliza a {@code List<String>} para
     * comparar contra los UUIDs de rol originales.
     */
    public static List<String> stringListClaim(JsonWebToken jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        List<String> out = new ArrayList<>();
        if (claim instanceof JsonArray array) {
            for (JsonValue v : array) {
                out.add(v instanceof JsonString js ? js.getString() : v.toString());
            }
        } else if (claim instanceof Collection<?> col) {
            for (Object o : col) {
                out.add(o instanceof JsonString js ? js.getString() : String.valueOf(o));
            }
        }
        return out;
    }

    /** Llave pública de test (X.509) commiteada en el classpath de test. */
    public static PublicKey testPublicKey() {
        try {
            byte[] der = pemBody(readResource("publicKey.pem"));
            return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar publicKey.pem de test", e);
        }
    }

    /** Llave privada de test (PKCS#8) commiteada en el classpath de test. */
    public static PrivateKey testPrivateKey() {
        try {
            byte[] der = pemBody(readResource("privateKey.pem"));
            return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar privateKey.pem de test", e);
        }
    }

    /** Genera un par RSA efímero (para probar rechazo de firma con otra llave). */
    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readResource(String name) throws Exception {
        try (InputStream is = JwtTestSupport.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) throw new IllegalStateException("Recurso de test no encontrado: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] pemBody(String pem) {
        String body = pem
            .replaceAll("-----BEGIN (.*)-----", "")
            .replaceAll("-----END (.*)-----", "")
            .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(body);
    }
}
