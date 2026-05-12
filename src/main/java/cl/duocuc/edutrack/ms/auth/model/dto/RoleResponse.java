package cl.duocuc.edutrack.ms.auth.model.dto;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
