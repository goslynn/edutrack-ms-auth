package cl.duocuc.edutrack.ms.auth.service.boostrap;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.model.entity.UserRole;
import cl.duocuc.edutrack.ms.auth.repository.UserRepository;
import cl.duocuc.edutrack.ms.auth.service.PasswordService;
import cl.duocuc.edutrack.ms.infrastructure.persistence.AuditContext;
import com.oracle.svm.core.annotate.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NoopUserSeeder {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @Transactional
    public void seedIfNeeded() {
        boolean hayNoop = userRepository.findByIdOptional(AuditContext.NOOP_USER_ID)
                .isPresent();

        if (hayNoop)
            return;

        User noop = new User();
        noop.id = AuditContext.NOOP_USER_ID;
        noop.email = "noop@sample.email";
        noop.passwordHash = passwordService.hash("no-passw");
        noop.displayName = "noop";
        noop.enabled = false;
        noop.creatorUser = AuditContext.NOOP_USER_ID;
        noop.updaterUser = AuditContext.NOOP_USER_ID;
        noop.persist();

    }

}
