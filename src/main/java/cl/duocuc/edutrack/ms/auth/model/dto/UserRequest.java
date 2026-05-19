package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @JsonView(Views.Create.class)
    @Email @Size(max = 255)
    @NotBlank(groups = Validations.OnCreate.class) String email,

    @JsonView(Views.Create.class)
    @Size(min = 8, max = 128)
    @NotBlank(groups = Validations.OnCreate.class) String password,

    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Size(max = 30)
    @NotBlank(groups = Validations.OnCreate.class) String displayName,

    @JsonView({Views.Update.class, Views.Patch.class, Views.Admin.class})
    Boolean enabled
) {}
