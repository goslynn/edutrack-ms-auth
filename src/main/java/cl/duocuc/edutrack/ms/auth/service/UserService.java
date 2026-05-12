package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.dto.UserResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.repository.RoleRepository;
import cl.duocuc.edutrack.ms.auth.model.repository.UserRepository;
import cl.duocuc.edutrack.ms.auth.model.repository.UserRoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        return User.listAll();
    }

    public User findById(UUID id) {
        return (User) User.findByIdOptional(id)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Transactional
    public User update(UUID id, String displayName, Boolean enabled) {
        User user = findById(id);
        if (displayName != null) user.displayName = displayName;
        if (enabled != null) {
            if (Boolean.FALSE.equals(enabled)) guardLastSuperuser(id);
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
        List<UUID> roleIds = userRoleRepository.findRoleIdsByUserId(user.id);
        return new UserResponse(
            user.id, user.email, user.displayName,
            user.enabled, user.createdAt, user.updatedAt, roleIds
        );
    }

    private void guardLastSuperuser(UUID userId) {
        roleRepository.findByName("SUPERUSER").ifPresent(sr -> {
            if (userRoleRepository.existsAssignment(userId, sr.id)) {
                long remaining = userRoleRepository.countActiveByRoleIdExcluding(sr.id, userId);
                if (remaining == 0) {
                    throw new WebApplicationException(Response.status(409)
                        .entity(Map.of("error", "Cannot disable the last active SUPERUSER"))
                        .build());
                }
            }
        });
    }
}
