package cl.duocuc.edutrack.ms.auth.model.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 30) String displayName,
    Boolean enabled
) {}
