package cl.duocuc.edutrack.ms.auth.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_roles", schema = "auth")
public class UserRole extends PanacheEntityBase {

    @EmbeddedId
    public UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    public Role role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    public Instant assignedAt;

    public UserRole() {}

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.id = new UserRoleId(user.id, role.id);
    }

    @PrePersist
    void prePersist() {
        assignedAt = Instant.now();
    }
}
