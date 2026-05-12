package cl.duocuc.edutrack.ms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}
