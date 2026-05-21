package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.entity.UserRole;
import cl.duocuc.edutrack.ms.auth.repository.RoleRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class UserRoleService {

    @Inject
    UserRoleRepository userRoleRepository;

    @Inject
    RoleRepository roleRepository;

    /** Retorna los roles completos usando join fetch para evitar lazy loading fuera de transacción. */
    public List<Role> findRolesByUser(UUID userId) {
        return userRoleRepository.getEntityManager()
            .createQuery(
                "select ur.role from UserRole ur where ur.id.userId = :userId",
                Role.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    @Transactional
    public void assign(UUID userId, UUID roleId) {
        User user = (User) User.findByIdOptional(userId)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        Role role = (Role) Role.findByIdOptional(roleId)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        if (userRoleRepository.existsAssignment(userId, roleId)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        new UserRole(user, role).persist();
    }

    @Transactional
    public void revoke(UUID userId, UUID roleId) {
        if (!userRoleRepository.existsAssignment(userId, roleId)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        roleRepository.findByName("SUPERUSER").ifPresent(sr -> {
            if (roleId.equals(sr.id)) {
                long remaining = userRoleRepository.countActiveByRoleIdExcluding(sr.id, userId);
                if (remaining == 0) {
                    throw new WebApplicationException(Response.status(409)
                        .entity(Map.of("error", "Cannot revoke SUPERUSER role from the last active superuser"))
                        .build());
                }
            }
        });

        userRoleRepository.delete("id.userId = ?1 and id.roleId = ?2", userId, roleId);
    }
}
