package cl.duocuc.edutrack.ms.infrastructure.exception;

/** Sugar para {@code DomainException} con status {@code 404 Not Found}. */
public class NotFoundException extends DomainException {
    public NotFoundException(String code, String message) {
        super(404, code, message);
    }
}
