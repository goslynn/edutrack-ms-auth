package cl.duocuc.edutrack.ms.infrastructure.exception;

/** Sugar para {@code DomainException} con status {@code 409 Conflict}. */
public class ConflictException extends DomainException {
    public ConflictException(String code, String message) {
        super(409, code, message);
    }
}
