package cl.duocuc.edutrack.ms.auth.model.entity;

import cl.duocuc.edutrack.ms.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(
    name = "role_permissions",
    schema = "auth",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions_role_resource", columnNames = {"role_id", "resource_key"})
)
public class RolePermission extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;

    // Clave estable de texto — opaca, el MS dueño del recurso la define en código
    @Column(name = "resource_key", nullable = false)
    public String resourceKey;

    // Flags Unix-style: r=4, w=2, x=1 (suma de flags activos, rango 0–7)
    @Column(nullable = false)
    public short flags;
}
