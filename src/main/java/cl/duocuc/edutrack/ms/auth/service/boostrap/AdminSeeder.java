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

@ApplicationScoped
public class AdminSeeder implements Seeder {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PasswordService passwordService;

    @Inject
    AdminSeedConfig config;

    @Override
    @Transactional
    public void seed() {
        Role superuserRole = roleRepository.findByName("SUPERUSER")
            .orElseThrow(() -> new IllegalStateException(
                "SUPERUSER role not found — verify Flyway V2 migration ran correctly"));

        boolean hayAdmin = userRepository.findByEmail(config.email())
                .filter(u -> u.userRoles.stream().anyMatch(ur -> superuserRole.equals(ur.role)))
                .isPresent();

        if (hayAdmin)
            return;

        User admin = new User();
        admin.email = config.email();
        admin.passwordHash = passwordService.hash(config.password());
        admin.displayName = config.displayName();
        admin.enabled = true;
        admin.persist();

        new UserRole(admin, superuserRole).persist();
    }
}
