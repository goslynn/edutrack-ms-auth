package cl.duocuc.edutrack.ms.auth.endpoint;

import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.repository.RoleRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Base helper para tests E2E. Expone los UUIDs de roles semilla (V2) y del admin
 * seedeado por AdminSeeder. Las pruebas spoofan los headers `X-User-Roles` y
 * `X-User-Id` igual que lo haría el API Gateway tras validar el JWT.
 */
public abstract class EndpointTestSupport {

    @Inject
    RoleRepository roleRepository;

    @Inject
    UserRepository userRepository;

    protected UUID superuserRoleId() {
        return resolveRoleId("SUPERUSER");
    }

    protected String superuserHeader() {
        return resolveRoleId("SUPERUSER").toString();
    }

    protected String adminHeader() {
        return resolveRoleId("ADMIN").toString();
    }

    protected String docenteHeader() {
        return resolveRoleId("DOCENTE").toString();
    }

    protected UUID resolveRoleId(String name) {
        return roleRepository.findByName(name).orElseThrow().id;
    }

    protected UUID adminUserId() {
        return userRepository.findByEmail("admin@edutrack.cl").orElseThrow().id;
    }

    protected User adminUser() {
        return userRepository.findByEmail("admin@edutrack.cl").orElseThrow();
    }

    protected String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.local";
    }
}
