package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.UUID;

public record PermissionResponse(
    @JsonView(Views.Base.class) UUID roleId,
    @JsonView(Views.Base.class) UUID resourceUuid,
    @JsonView(Views.Base.class) short flags,
    @JsonView({Views.Base.class, Views.Extra.class}) String flagsLabel
) {
    public static String toLabel(short flags) {
        return ((flags & 4) != 0 ? "r" : "-")
             + ((flags & 2) != 0 ? "w" : "-")
             + ((flags & 1) != 0 ? "x" : "-");
    }
}
