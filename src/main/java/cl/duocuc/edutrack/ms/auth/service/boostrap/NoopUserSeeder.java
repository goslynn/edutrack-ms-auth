package cl.duocuc.edutrack.ms.auth.service.boostrap;

import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import cl.duocuc.edutrack.ms.auth.service.PasswordService;
import cl.duocuc.edutrack.ms.infrastructure.persistence.AuditContext;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class NoopUserSeeder {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @Transactional
    public void seedIfNeeded() {
        final UUID noopId = AuditContext.props().noopUserId();

        boolean hayNoop = userRepository.findByIdOptional(noopId)
                .isPresent();

        if (hayNoop)
            return;

        User noop = new User();
        noop.id = noopId;
        noop.email = "noop@sample.email";
        noop.passwordHash = passwordService.hash("no-passw");
        noop.displayName = "noop";
        noop.enabled = false;
        noop.creatorUser = noopId;
        noop.updaterUser = noopId;
        noop.persist();

    }

}
