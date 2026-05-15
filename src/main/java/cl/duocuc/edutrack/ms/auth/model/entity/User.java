package cl.duocuc.edutrack.ms.auth.model.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", schema = "auth")
public class User extends AuditableEntity {

    @Column(name = "display_name", nullable = false, length = 30)
    public String displayName;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public boolean enabled = true;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    public List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    public List<RefreshToken> refreshTokens = new ArrayList<>();
}
