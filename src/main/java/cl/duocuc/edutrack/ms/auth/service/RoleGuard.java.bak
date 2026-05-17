package cl.duocuc.edutrack.ms.auth.service;

import cl.duocuc.edutrack.ms.auth.model.repository.RoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RoleGuard {

    @Inject
    RoleRepository roleRepository;

    /**
     * Verifica que el header X-User-Roles (lista de UUIDs separados por coma)
     * contenga al menos uno de los roles permitidos por nombre.
     * Lanza 403 si la validación falla.
     */
    public void requireAnyRole(String rolesHeader, String... allowedRoleNames) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        List<UUID> roleIds = Arrays.stream(rolesHeader.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> {
                try { return UUID.fromString(s); }
                catch (IllegalArgumentException e) {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
            })
            .toList();

        if (roleIds.isEmpty()) throw new WebApplicationException(Response.Status.FORBIDDEN);

        List<String> userRoleNames = roleRepository.findNamesByIds(roleIds);
        Set<String> allowed = Set.of(allowedRoleNames);

        if (userRoleNames.stream().noneMatch(allowed::contains)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }
}
