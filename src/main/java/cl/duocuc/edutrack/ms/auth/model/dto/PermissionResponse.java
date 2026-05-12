package cl.duocuc.edutrack.ms.auth.model.dto;

import java.util.UUID;

public record PermissionResponse(
    UUID roleId,
    UUID resourceUuid,
    short flags,
    String flagsLabel
) {
    public static String toLabel(short flags) {
        return ((flags & 4) != 0 ? "r" : "-")
             + ((flags & 2) != 0 ? "w" : "-")
             + ((flags & 1) != 0 ? "x" : "-");
    }
}
