package cl.duocuc.edutrack.ms.auth.model.entity;

import cl.duocuc.edutrack.ms.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
    name = "role_permissions",
    schema = "auth",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions_role_resource", columnNames = {"role_id", "resource_uuid"})
)
public class RolePermission extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;

    // Opaque UUID — sin FK, el MS dueño del recurso gestiona este identificador
    @Column(name = "resource_uuid", nullable = false, columnDefinition = "uuid")
    public UUID resourceUuid;

    // Flags Unix-style: r=4, w=2, x=1 (suma de flags activos, rango 0–7)
    @Column(nullable = false)
    public short flags;
}
