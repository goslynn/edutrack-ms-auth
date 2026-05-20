package cl.duocuc.edutrack.ms.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@MappedSuperclass
public abstract class AuditableEntity extends CreatableEntity {

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Override
    protected void onCreate() {
        super.onCreate();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
