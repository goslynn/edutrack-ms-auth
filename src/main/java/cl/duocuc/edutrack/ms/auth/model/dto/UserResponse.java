package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    @JsonView(Views.Base.class) UUID id,
    @JsonView(Views.Base.class) String email,
    @JsonView(Views.Base.class) String displayName,
    @JsonView({Views.Base.class, Views.Admin.class}) boolean enabled,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant createdAt,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant updatedAt,
    @JsonView({Views.Detailed.class, Views.Admin.class, Views.Extra.class}) List<UUID> roleIds
) {}
