package cl.duocuc.edutrack.ms.auth.repository;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<Role, UUID> {

    public Optional<Role> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

//    public List<String> findNamesByIds(List<UUID> ids) {
//        if (ids == null || ids.isEmpty()) return List.of();
//        return getEntityManager()
//            .createQuery("select r.name from Role r where r.id in :ids", String.class)
//            .setParameter("ids", ids)
//            .getResultList();
//    }
}
