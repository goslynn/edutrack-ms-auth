package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PermissionRequest(
    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Min(0) @Max(7) short flags
) {}
