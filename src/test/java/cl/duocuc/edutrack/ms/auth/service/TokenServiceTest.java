package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.RefreshToken;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.repository.RefreshTokenRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRoleRepository;
import cl.duocuc.edutrack.ms.auth.support.JwtTestSupport;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del {@code TokenService} — emisión RS256 y rotación/revocación de
 * refresh tokens. Atómicos y agnósticos: se mockea la capa de persistencia
 * (repositorios Panache) y la construcción/persistencia del {@code RefreshToken}
 * (vía {@link org.mockito.Mockito#mockConstruction}), de modo que no hace falta
 * una base de datos. La firma del access token sí es real: el {@code Jwt.sign()}
 * sin argumentos toma la llave de {@code smallrye.jwt.sign.key.location}
 * (definida para tests en {@code META-INF/microprofile-config.properties}).
 *
 * <p>Aquí viven los requisitos 1.2 (expirado) y 1.3 (invalidado) a nivel del
 * refresh token: el access token JWT es stateless y de vida corta (15 min); la
 * sesión renovable se invalida revocando el refresh token, y un refresh token
 * revocado o expirado no puede rotarse.</p>
 */
class TokenServiceTest {

    private UserRoleRepository userRoleRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private TokenService service;

    private final UUID userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final List<UUID> roleIds = List.of(
        UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"),
        UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002"));

    @BeforeEach
    void setUp() {
        userRoleRepository = mock(UserRoleRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        service = new TokenService();
        service.userRoleRepository = userRoleRepository;
        service.refreshTokenRepository = refreshTokenRepository;
        service.accessExpirySeconds = 900;
        service.refreshExpiryDays = 7;
    }

    private User user() {
        User u = new User();
        u.id = userId;
        u.email = "user@edutrack.cl";
        u.enabled = true;
        return u;
    }

    @Test
    @DisplayName("1.1 — issueTokenPair emite un access token RS256 verificable con sub y roles del usuario")
    void issueTokenPair_emitsVerifiableAccessToken() throws ParseException {
        when(userRoleRepository.findRoleIdsByUserId(userId)).thenReturn(roleIds);

        AuthResponse response;
        try (MockedConstruction<RefreshToken> ignored = mockConstruction(RefreshToken.class)) {
            response = service.issueTokenPair(user());
        }

        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());

        JsonWebToken jwt = JwtTestSupport.verify(response.accessToken());
        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(JwtTestSupport.ISSUER, jwt.getIssuer());

        List<String> roles = JwtTestSupport.stringListClaim(jwt, "roles");
        List<String> expected = roleIds.stream().map(UUID::toString).toList();
        assertEquals(expected.size(), roles.size());
        assertTrue(roles.containsAll(expected), "el claim roles debe reflejar los roles del usuario");
    }

    @Test
    @DisplayName("issueTokenPair persiste el refresh token HASHEADO (nunca el valor en claro) y con expiración futura")
    void issueTokenPair_persistsHashedRefreshToken() {
        when(userRoleRepository.findRoleIdsByUserId(userId)).thenReturn(roleIds);

        AuthResponse response;
        try (MockedConstruction<RefreshToken> mocked = mockConstruction(RefreshToken.class)) {
            response = service.issueTokenPair(user());

            assertEquals(1, mocked.constructed().size(), "debe persistirse exactamente un refresh token");
            RefreshToken persisted = mocked.constructed().get(0);

            String raw = response.refreshToken();
            assertNotEquals(raw, persisted.tokenHash, "no se debe almacenar el token en claro");
            assertEquals(service.hashToken(raw), persisted.tokenHash, "se almacena el hash SHA-256 del token");
            assertFalse(persisted.revoked, "un token recién emitido no está revocado");
            assertNotNull(persisted.expiresAt);
            assertTrue(persisted.expiresAt.isAfter(Instant.now()), "el refresh token expira en el futuro");
            assertEquals(userId, persisted.user.id);
            verify(persisted, times(1)).persist();
        }
    }

    @Test
    @DisplayName("hashToken es SHA-256 hex determinista y distinto del valor en claro")
    void hashToken_isDeterministicSha256() {
        String raw = "some-raw-refresh-token";
        String h1 = service.hashToken(raw);
        String h2 = service.hashToken(raw);

        assertEquals(h1, h2, "el hash debe ser determinista");
        assertEquals(64, h1.length(), "SHA-256 en hex son 64 caracteres");
        assertNotEquals(raw, h1);
        assertNotEquals(h1, service.hashToken("another-token"), "tokens distintos producen hashes distintos");
    }

    @Test
    @DisplayName("rotateRefreshToken rechaza (403) un token desconocido")
    void rotateRefreshToken_unknownToken_forbidden() {
        when(refreshTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(Optional.empty());

        WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> service.rotateRefreshToken("does-not-exist"));
        assertEquals(403, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("1.3 — rotateRefreshToken rechaza (403) un token revocado (sesión invalidada por logout)")
    void rotateRefreshToken_revokedToken_forbidden() {
        RefreshToken revoked = new RefreshToken();
        revoked.user = user();
        revoked.revoked = true;
        revoked.revokedAt = Instant.now();
        revoked.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS); // aún no expira, pero está revocado
        when(refreshTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(Optional.of(revoked));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> service.rotateRefreshToken("raw"));
        assertEquals(403, ex.getResponse().getStatus());
        // No se emite un nuevo par a partir de un token revocado.
        verify(userRoleRepository, never()).findRoleIdsByUserId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("1.2 — rotateRefreshToken rechaza (403) un token expirado")
    void rotateRefreshToken_expiredToken_forbidden() {
        RefreshToken expired = new RefreshToken();
        expired.user = user();
        expired.revoked = false;
        expired.expiresAt = Instant.now().minus(1, ChronoUnit.DAYS);
        when(refreshTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(Optional.of(expired));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> service.rotateRefreshToken("raw"));
        assertEquals(403, ex.getResponse().getStatus());
        verify(userRoleRepository, never()).findRoleIdsByUserId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("rotateRefreshToken con un token válido revoca el anterior y emite un par nuevo")
    void rotateRefreshToken_validToken_rotates() {
        // Token "encontrado" construido FUERA del scope de mockConstruction (objeto real).
        RefreshToken active = new RefreshToken();
        active.user = user();
        active.revoked = false;
        active.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        when(refreshTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(Optional.of(active));
        when(userRoleRepository.findRoleIdsByUserId(userId)).thenReturn(roleIds);

        AuthResponse response;
        try (MockedConstruction<RefreshToken> ignored = mockConstruction(RefreshToken.class)) {
            response = service.rotateRefreshToken("valid-raw");
        }

        assertTrue(active.revoked, "el refresh token usado debe quedar revocado");
        assertNotNull(active.revokedAt);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
    }
}
