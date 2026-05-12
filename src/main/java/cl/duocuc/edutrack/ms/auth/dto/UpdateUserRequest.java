package cl.duocuc.edutrack.ms.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 30) String displayName,
    Boolean enabled
) {}
