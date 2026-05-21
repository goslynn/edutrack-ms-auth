package cl.duocuc.edutrack.ms.auth.service.boostrap;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.entity.UserRole;
import cl.duocuc.edutrack.ms.auth.repository.RoleRepository;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import cl.duocuc.edutrack.ms.auth.service.PasswordService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AdminSeeder {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PasswordService passwordService;

    @ConfigProperty(name = "auth.seed.admin.email", defaultValue = "admin@edutrack.cl")
    String adminEmail;

    @ConfigProperty(name = "auth.seed.admin.password", defaultValue = "changeme123!")
    String adminPassword;

    @ConfigProperty(name = "auth.seed.admin.display-name", defaultValue = "admin")
    String adminDisplayName;

    @Transactional
    public void seedIfNeeded() {
        Role superuserRole = roleRepository.findByName("SUPERUSER")
            .orElseThrow(() -> new IllegalStateException(
                "SUPERUSER role not found — verify Flyway V2 migration ran correctly"));

        boolean hayAdmin = userRepository.findByEmail(adminEmail)
                .filter(u -> u.userRoles.stream().anyMatch(ur -> superuserRole.equals(ur.role)))
                .isPresent();

        if (hayAdmin)
            return;

        User admin = new User();
        admin.email = adminEmail;
        admin.passwordHash = passwordService.hash(adminPassword);
        admin.displayName = adminDisplayName;
        admin.enabled = true;
        admin.persist();

        new UserRole(admin, superuserRole).persist();
    }
}
