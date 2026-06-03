package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.dto.PermissionResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.auth.repository.RolePermissionRepository;
import cl.duocuc.edutrack.ms.infrastructure.security.PermissionEvaluator;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PermissionService implements PermissionEvaluator {

    @Inject
    RolePermissionRepository permissionRepository;

    @Transactional
    public RolePermission upsert(UUID roleId, String resourceKey, short flags) {
        Role role = (Role) Role.findByIdOptional(roleId)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        return permissionRepository.findByRoleAndResource(roleId, resourceKey)
            .map(existing -> {
                existing.flags = flags;
                return existing;
            })
            .orElseGet(() -> {
                RolePermission perm = new RolePermission();
                perm.role = role;
                perm.resourceKey = resourceKey;
                perm.flags = flags;
                perm.persist();
                return perm;
            });
    }

    public List<RolePermission> listByRole(UUID roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    @Transactional
    public void delete(UUID roleId, String resourceKey) {
        RolePermission perm = permissionRepository.findByRoleAndResource(roleId, resourceKey)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        perm.delete();
    }

    public short computeEffectiveFlags(List<UUID> roleIds, String resourceKey) {
        return (short) permissionRepository.findByRolesAndResource(roleIds, resourceKey)
                .stream().mapToInt(p -> p.flags)
                .reduce(0, (a, b) -> a | b);
    }

    /**
     * Flags efectivos de un conjunto de roles sobre un recurso, incluyendo el
     * comodín {@link ResourceIds#ALL} (un grant sobre {@code ALL} — p. ej.
     * SUPERUSER — cubre cualquier recurso). Es el mismo cálculo que aplica
     * {@code RequirePermissionFilter}.
     */
    public short effectiveFlags(List<UUID> roleIds, String resourceKey) {
        short concrete = computeEffectiveFlags(roleIds, resourceKey);
        short wildcard = computeEffectiveFlags(roleIds, ResourceIds.ALL);
        return (short) (concrete | wildcard);
    }

    /**
     * ¿Los roles satisfacen el bit requerido sobre el recurso? Fuente de verdad
     * única del algoritmo que materializa {@code @RequirePermission}: lo usan
     * tanto el filtro (vía {@link PermissionEvaluator}) como el endpoint público
     * de verificación.
     */
    @Override
    public boolean hasPermission(List<UUID> roleIds, String resourceKey, short requiredBits) {
        return (effectiveFlags(roleIds, resourceKey) & requiredBits) == requiredBits;
    }

    public PermissionResponse toResponse(RolePermission perm) {
        return PermissionResponse.fromEntity(perm);
    }
}
