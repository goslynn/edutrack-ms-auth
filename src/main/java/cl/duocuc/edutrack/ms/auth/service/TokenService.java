package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.LoginResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.RefreshToken;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.repository.RefreshTokenRepository;
import cl.duocuc.edutrack.ms.auth.model.repository.UserRoleRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "auth.jwt.access-token.expiry-seconds", defaultValue = "900")
    long accessExpirySeconds;

    @ConfigProperty(name = "auth.jwt.refresh-token.expiry-days", defaultValue = "7")
    long refreshExpiryDays;

    @Inject
    UserRoleRepository userRoleRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse issueTokenPair(User user) {
        List<UUID> roleIds = userRoleRepository.findRoleIdsByUserId(user.id);
        String accessToken = buildAccessToken(user.id, roleIds);
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken rt = new RefreshToken();
        rt.user = user;
        rt.tokenHash = hashToken(rawRefreshToken);
        rt.expiresAt = Instant.now().plus(refreshExpiryDays, ChronoUnit.DAYS);
        rt.persist();

        return new LoginResponse(accessToken, rawRefreshToken, "Bearer", accessExpirySeconds);
    }

    @Transactional
    public LoginResponse rotateRefreshToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new WebApplicationException(Response.Status.FORBIDDEN));

        if (rt.revoked || rt.expiresAt.isBefore(Instant.now())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        rt.revoked = true;
        rt.revokedAt = Instant.now();

        return issueTokenPair(rt.user);
    }

    private String buildAccessToken(UUID userId, List<UUID> roleIds) {
        List<String> roles = roleIds.stream().map(UUID::toString).toList();
        return Jwt.issuer("edutrack-auth")
            .subject(userId.toString())
            .claim("roles", roles)
            .expiresAt(Instant.now().plusSeconds(accessExpirySeconds))
            .sign();
    }

    public String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
