package cl.duocuc.edutrack.ms.auth.model.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.UserRole;
import cl.duocuc.edutrack.ms.auth.model.entity.UserRoleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserRoleRepository implements PanacheRepositoryBase<UserRole, UserRoleId> {

    public List<UserRole> findByUserId(UUID userId) {
        return list("id.userId", userId);
    }

    /** Returns only the role UUIDs — suitable for embedding in JWT claims. */
    public List<UUID> findRoleIdsByUserId(UUID userId) {
        return getEntityManager()
            .createQuery(
                "select ur.id.roleId from UserRole ur where ur.id.userId = ?1",
                UUID.class)
            .setParameter(1, userId)
            .getResultList();
    }

    public boolean existsAssignment(UUID userId, UUID roleId) {
        return count("id.userId = ?1 and id.roleId = ?2", userId, roleId) > 0;
    }

    public long deleteByUserId(UUID userId) {
        return delete("id.userId", userId);
    }

    public long countActiveByRoleIdExcluding(UUID roleId, UUID excludeUserId) {
        return getEntityManager()
            .createQuery(
                "select count(ur) from UserRole ur join User u on u.id = ur.id.userId " +
                "where ur.id.roleId = :roleId and u.enabled = true and u.id <> :excludeUserId",
                Long.class)
            .setParameter("roleId", roleId)
            .setParameter("excludeUserId", excludeUserId)
            .getSingleResult();
    }
}
