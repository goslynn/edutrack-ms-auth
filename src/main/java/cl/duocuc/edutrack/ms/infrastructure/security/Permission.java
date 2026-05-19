package cl.duocuc.edutrack.ms.infrastructure.security;

/**
 * Bits de permiso Unix-style usados como mínimo requerido por un endpoint.
 * {@code r=4, w=2, x=1}; coinciden con el {@code flags SMALLINT} de
 * {@code auth.role_permissions}.
 */
public enum Permission {
    READ((short) 4),
    WRITE((short) 2),
    EXECUTE((short) 1);

    public final short bit;

    Permission(short bit) {
        this.bit = bit;
    }
}
