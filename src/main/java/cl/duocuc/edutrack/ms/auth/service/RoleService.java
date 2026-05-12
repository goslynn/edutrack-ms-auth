package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.dto.RoleResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.UserRole;
import cl.duocuc.edutrack.ms.auth.model.repository.RoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RoleService {

    @Inject
    RoleRepository roleRepository;

    @Transactional
    public Role create(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        Role role = new Role();
        role.name = name;
        role.description = description;
        role.persist();
        return role;
    }

    public List<Role> listAll() {
        return Role.listAll();
    }

    public Role findById(UUID id) {
        return (Role) Role.findByIdOptional(id)
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @Transactional
    public Role update(UUID id, String name, String description) {
        Role role = findById(id);
        if (name != null && !name.equals(role.name)) {
            if (roleRepository.existsByName(name)) {
                throw new WebApplicationException(Response.Status.CONFLICT);
            }
            role.name = name;
        }
        if (description != null) role.description = description;
        return role;
    }

    @Transactional
    public void delete(UUID id) {
        Role role = findById(id);
        long assignedCount = UserRole.count("id.roleId", id);
        if (assignedCount > 0) {
            throw new WebApplicationException(Response.status(409)
                .entity(Map.of("error", "Role is still assigned to " + assignedCount + " user(s)"))
                .build());
        }
        role.delete();
    }

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(role.id, role.name, role.description, role.createdAt, role.updatedAt);
    }
}
