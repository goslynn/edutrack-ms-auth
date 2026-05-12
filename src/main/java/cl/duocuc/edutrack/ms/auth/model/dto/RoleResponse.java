package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
    @JsonView(Views.Base.class) UUID id,
    @JsonView(Views.Base.class) String name,
    @JsonView({Views.Base.class, Views.Extra.class}) String description,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant createdAt,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant updatedAt
) {}
