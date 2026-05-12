package cl.duocuc.edutrack.ms.auth.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt,
    List<UUID> roleIds
) {}
