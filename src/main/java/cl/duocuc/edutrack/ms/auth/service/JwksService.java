package cl.duocuc.edutrack.ms.auth.service;

import io.smallrye.jwt.util.ResourceUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

@ApplicationScoped
public class JwksService {

    /**
     * Misma ubicacion que consume el verificador MP-JWT: la llave ya no vive en
     * el classpath sino en .certs/ (inyectado por env var). Se resuelve con el
     * mismo {@link ResourceUtils} de SmallRye que usa el verificador y el
     * firmador (classpath/file/URL), de modo que el JWKS publica exactamente la
     * llave con la que se validan los tokens.
     */
    @ConfigProperty(name = "mp.jwt.verify.publickey.location")
    String publicKeyLocation;

    private String jwksJson;

    @PostConstruct
    void init() {
        try (InputStream is = ResourceUtils.getResourceStream(publicKeyLocation)) {
            if (is == null) throw new IllegalStateException(
                "Public key not found at " + publicKeyLocation);

            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));

            String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toUnsignedBytes(publicKey.getModulus()));
            String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toUnsignedBytes(publicKey.getPublicExponent()));

            jwksJson = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\","
                + "\"kid\":\"edutrack-auth-key-1\","
                + "\"n\":\"" + n + "\","
                + "\"e\":\"" + e + "\"}]}";
        } catch (Exception ex) {
            throw new RuntimeException(
                "Failed to load public key for JWKS endpoint from " + publicKeyLocation, ex);
        }
    }

    public String getJwks() {
        return jwksJson;
    }

    private byte[] toUnsignedBytes(BigInteger value) {
        byte[] bytes = value.toByteArray();
        return (bytes[0] == 0) ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
    }
}
