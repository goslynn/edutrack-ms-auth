package cl.duocuc.edutrack.ms.auth.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "role_permissions",
    schema = "auth",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions_role_resource", columnNames = {"role_id", "resource_uuid"})
)
public class RolePermission extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;

    // Opaque UUID — sin FK, el MS dueño del recurso gestiona este identificador
    @Column(name = "resource_uuid", nullable = false, columnDefinition = "uuid")
    public UUID resourceUuid;

    // Flags Unix-style: r=4, w=2, x=1 (suma de flags activos, rango 0–7)
    @Column(nullable = false)
    public short flags;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
