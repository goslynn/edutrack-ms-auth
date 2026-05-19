package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(
    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Size(max = 100)
    @NotBlank(groups = Validations.OnCreate.class) String name,

    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Size(max = 500) String description
) {}
