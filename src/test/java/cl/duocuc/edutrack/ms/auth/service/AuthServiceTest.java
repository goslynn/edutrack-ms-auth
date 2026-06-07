package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.repository.RefreshTokenRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del {@code AuthService} — login, refresh y logout. Atómicos: todas las
 * colaboraciones (usuarios, password, emisión de tokens, revocación) están
 * mockeadas. Verifica el contrato de autenticación:
 * <ul>
 *   <li>credenciales inválidas / usuario deshabilitado / inexistente ⇒ 401,
 *       sin revelar cuál de las condiciones falló y sin emitir tokens;</li>
 *   <li>logout revoca TODOS los refresh tokens del usuario (1.3 — invalidación
 *       de la sesión renovable);</li>
 *   <li>refresh delega en la rotación del {@code TokenService}.</li>
 * </ul>
 */
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordService passwordService;
    private TokenService tokenService;
    private RefreshTokenRepository refreshTokenRepository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordService = mock(PasswordService.class);
        tokenService = mock(TokenService.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        service = new AuthService();
        service.userRepository = userRepository;
        service.passwordService = passwordService;
        service.tokenService = tokenService;
        service.refreshTokenRepository = refreshTokenRepository;
    }

    private User enabledUser(String email, String hash) {
        User u = new User();
        u.id = UUID.randomUUID();
        u.email = email;
        u.passwordHash = hash;
        u.enabled = true;
        return u;
    }

    @Test
    @DisplayName("login con credenciales válidas emite un par de tokens")
    void login_validCredentials_issuesTokens() {
        User u = enabledUser("user@edutrack.cl", "hashed");
        AuthResponse expected = AuthResponse.of("access", "refresh", 900);
        when(userRepository.findByEmail("user@edutrack.cl")).thenReturn(Optional.of(u));
        when(passwordService.verify("secret", "hashed")).thenReturn(true);
        when(tokenService.issueTokenPair(u)).thenReturn(expected);

        AuthResponse result = service.login("user@edutrack.cl", "secret");

        assertSame(expected, result);
        verify(tokenService, times(1)).issueTokenPair(u);
    }

    @Test
    @DisplayName("login con password incorrecta ⇒ 401 y no emite tokens")
    void login_wrongPassword_unauthorized() {
        User u = enabledUser("user@edutrack.cl", "hashed");
        when(userRepository.findByEmail("user@edutrack.cl")).thenReturn(Optional.of(u));
        when(passwordService.verify("bad", "hashed")).thenReturn(false);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> service.login("user@edutrack.cl", "bad"));
        assertEquals(401, ex.getResponse().getStatus());
        verify(tokenService, never()).issueTokenPair(any());
    }

    @Test
    @DisplayName("login de un usuario deshabilitado ⇒ 401 (ni siquiera verifica la password)")
    void login_disabledUser_unauthorized() {
        User u = enabledUser("user@edutrack.cl", "hashed");
        u.enabled = false;
        when(userRepository.findByEmail("user@edutrack.cl")).thenReturn(Optional.of(u));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> service.login("user@edutrack.cl", "secret"));
        assertEquals(401, ex.getResponse().getStatus());
        verify(passwordService, never()).verify(anyString(), anyString());
        verify(tokenService, never()).issueTokenPair(any());
    }

    @Test
    @DisplayName("login con email inexistente ⇒ 401")
    void login_unknownEmail_unauthorized() {
        when(userRepository.findByEmail("ghost@edutrack.cl")).thenReturn(Optional.empty());

        WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> service.login("ghost@edutrack.cl", "secret"));
        assertEquals(401, ex.getResponse().getStatus());
        verify(passwordService, never()).verify(anyString(), anyString());
    }

    @Test
    @DisplayName("1.3 — logout revoca todos los refresh tokens del usuario")
    void logout_revokesAllRefreshTokens() {
        UUID userId = UUID.randomUUID();

        service.logout(userId);

        verify(refreshTokenRepository, times(1)).revokeAllByUserId(eq(userId));
    }

    @Test
    @DisplayName("refresh delega en la rotación del TokenService")
    void refresh_delegatesToRotation() {
        AuthResponse rotated = AuthResponse.of("new-access", "new-refresh", 900);
        when(tokenService.rotateRefreshToken("raw-refresh")).thenReturn(rotated);

        AuthResponse result = service.refresh("raw-refresh");

        assertSame(rotated, result);
        verify(tokenService, times(1)).rotateRefreshToken("raw-refresh");
    }
}
