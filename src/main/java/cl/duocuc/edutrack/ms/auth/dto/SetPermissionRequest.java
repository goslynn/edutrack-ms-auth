package cl.duocuc.edutrack.ms.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SetPermissionRequest(
    @Min(0) @Max(7) short flags
) {}
