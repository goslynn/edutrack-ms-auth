package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.repository.RefreshTokenRepository;
import cl.duocuc.edutrack.ms.auth.model.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @Inject
    TokenService tokenService;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .filter(u -> u.enabled)
            .orElseThrow(() -> new WebApplicationException(Response.Status.UNAUTHORIZED));

        if (!passwordService.verify(password, user.passwordHash)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        return tokenService.issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        return tokenService.rotateRefreshToken(rawRefreshToken);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
