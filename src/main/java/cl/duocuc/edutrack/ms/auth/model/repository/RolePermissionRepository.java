package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RolePermissionRepository implements PanacheRepositoryBase<RolePermission, UUID> {

    public Optional<RolePermission> findByRoleAndResource(UUID roleId, UUID resourceUuid) {
        return find("role.id = ?1 and resourceUuid = ?2", roleId, resourceUuid)
            .firstResultOptional();
    }

    public List<RolePermission> findByRoleId(UUID roleId) {
        return list("role.id", roleId);
    }

    /**
     * Computes the effective permission flags for a resource given a set of roles.
     * Effective flags = bitwise OR of each role's flags (BE-AUTH-005).
     */
    public short computeEffectiveFlags(List<UUID> roleIds, UUID resourceUuid) {
        if (roleIds == null || roleIds.isEmpty()) return 0;
        List<RolePermission> perms =
            list("role.id in ?1 and resourceUuid = ?2", roleIds, resourceUuid);
        return (short) perms.stream().mapToInt(p -> p.flags).reduce(0, (a, b) -> a | b);
    }
}
