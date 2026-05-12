package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.PermissionResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.auth.model.repository.RolePermissionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PermissionService {

    @Inject
    RolePermissionRepository permissionRepository;

    @Transactional
    public RolePermission upsert(UUID roleId, UUID resourceUuid, short flags) {
        if (flags < 0 || flags > 7) {
            throw new WebApplicationException(Response.status(422)
                .entity(java.util.Map.of("error", "flags must be between 0 and 7")).build());
        }
        Role role = (Role) Role.findByIdOptional(roleId)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        return permissionRepository.findByRoleAndResource(roleId, resourceUuid)
            .map(existing -> {
                existing.flags = flags;
                return existing;
            })
            .orElseGet(() -> {
                RolePermission perm = new RolePermission();
                perm.role = role;
                perm.resourceUuid = resourceUuid;
                perm.flags = flags;
                perm.persist();
                return perm;
            });
    }

    public List<RolePermission> listByRole(UUID roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    @Transactional
    public void delete(UUID roleId, UUID resourceUuid) {
        RolePermission perm = permissionRepository.findByRoleAndResource(roleId, resourceUuid)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        perm.delete();
    }

    public short computeEffectiveFlags(List<UUID> roleIds, UUID resourceUuid) {
        return permissionRepository.computeEffectiveFlags(roleIds, resourceUuid);
    }

    public PermissionResponse toResponse(RolePermission perm) {
        return new PermissionResponse(
            perm.role.id, perm.resourceUuid,
            perm.flags, PermissionResponse.toLabel(perm.flags)
        );
    }
}
