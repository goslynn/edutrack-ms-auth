package cl.duocuc.edutrack.ms.auth.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class RolePermissionRepository implements PanacheRepositoryBase<RolePermission, UUID> {

    public Optional<RolePermission> findByRoleAndResource(UUID roleId, String resourceKey) {
        return find("role.id = ?1 and resourceKey = ?2", roleId, resourceKey)
            .firstResultOptional();
    }

    public List<RolePermission> findByRoleId(UUID roleId) {
        return list("role.id", roleId);
    }

    public List<RolePermission> findByRolesAndResource(Collection<UUID> roleIds, String resourceKey) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptyList();
        return list("role.id in ?1 and resourceKey = ?2", roleIds, resourceKey);

    }

    /**
     * Computes the effective permission flags for a resource given a set of roles.
     * Effective flags = bitwise OR of each role's flags (BE-AUTH-005).
     */
    @Deprecated
    public short computeEffectiveFlags(List<UUID> roleIds, String resourceKey) {
        if (roleIds == null || roleIds.isEmpty()) return 0;
        List<RolePermission> perms =
            list("role.id in ?1 and resourceKey = ?2", roleIds, resourceKey);
        return (short) perms.stream().mapToInt(p -> p.flags).reduce(0, (a, b) -> a | b);
    }
}
