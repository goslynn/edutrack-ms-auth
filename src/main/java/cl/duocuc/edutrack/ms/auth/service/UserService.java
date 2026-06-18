package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.UserResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.repository.RoleRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRoleRepository;
import cl.duocuc.edutrack.ms.infrastructure.exception.BadRequestException;
import cl.duocuc.edutrack.ms.infrastructure.exception.ConflictException;
import cl.duocuc.edutrack.ms.infrastructure.exception.NotFoundException;
import cl.duocuc.edutrack.ms.infrastructure.persistence.AuditContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserRoleRepository userRoleRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PasswordService passwordService;

    @Transactional
    public User create(String email, String password, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        User user = new User();
        user.email = email;
        user.passwordHash = passwordService.hash(password);
        user.displayName = displayName;
        user.persist();
        return user;
    }

    public List<User> listAll() {
        return userRepository.listAll()
                .stream()
                .filter(Objects::nonNull)
                .filter(u -> !Objects.equals(u.id, AuditContext.props().noopUserId()))
                .toList();
    }

    public User findById(UUID id) {
        final UUID noopId = AuditContext.props().noopUserId();
        if (Objects.equals(id, noopId))
            throw new BadRequestException("uuid=" + id + "is reserved, cannot access.");

        return (User) User.findByIdOptional(id)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Transactional
    public User update(UUID id, String displayName, Boolean enabled) {
        User user = findById(id);
        if (displayName != null) user.displayName = displayName;
        if (enabled != null) {
            if (!enabled) guardLastSuperuser(id);
            user.enabled = enabled;
        }
        return user;
    }

    @Transactional
    public void disable(UUID id) {
        User user = findById(id);
        guardLastSuperuser(id);
        user.enabled = false;
    }

    public UserResponse toResponse(User user) {
        return UserResponse.fromEntity(user, userRoleRepository.findRoleIdsByUserId(user.id));
    }

    private void guardLastSuperuser(UUID userId) {
        roleRepository.findByName("SUPERUSER").ifPresent(sr -> {
            if (userRoleRepository.existsAssignment(userId, sr.id)) {
                long remaining = userRoleRepository.countActiveByRoleIdExcluding(sr.id, userId);
                if (remaining == 0) {
                    throw new ConflictException(
                        "AUTH.USER.LAST_SUPERUSER",
                        "Cannot disable the last active SUPERUSER")
                        .with("userId", userId);
                }
            }
        });
    }
}
