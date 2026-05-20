package cl.duocuc.edutrack.ms.infrastructure.exception;

/** Sugar para {@code DomainException} con status {@code 403 Forbidden}. */
public class ForbiddenException extends DomainException {
    public ForbiddenException(String code, String message) {
        super(403, code, message);
    }
}
