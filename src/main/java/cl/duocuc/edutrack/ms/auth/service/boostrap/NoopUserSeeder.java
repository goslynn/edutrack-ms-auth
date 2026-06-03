package cl.duocuc.edutrack.ms.auth.service.boostrap;

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

        // El id de la fila NOOP debe ser exactamente el sentinela configurado.
        // User hereda @GeneratedValue(GenerationType.UUID) de CreatableEntity, así
        // que un persist() de Panache (a) rechaza el id asignado como "detached" y
        // (b) lo sobreescribiría con un UUID aleatorio. Se inserta vía SQL nativo
        // —igual que el seed de roles en V2— para fijar el id sin disparar el
        // generador. created_at/updated_at toman su DEFAULT NOW() del DDL.
        userRepository.getEntityManager().createNativeQuery("""
                INSERT INTO auth.users
                    (id, email, password_hash, display_name, enabled, creator_user, updater_user)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                """)
            .setParameter(1, noopId)
            .setParameter(2, "noop@sample.email")
            .setParameter(3, passwordService.hash("no-passw"))
            .setParameter(4, "noop")
            .setParameter(5, false)
            .setParameter(6, noopId)
            .setParameter(7, noopId)
            .executeUpdate();
    }

}
