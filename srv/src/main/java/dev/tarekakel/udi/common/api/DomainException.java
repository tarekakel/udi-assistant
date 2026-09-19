package dev.tarekakel.udi.common.api;

import org.springframework.http.HttpStatus;

/**
 * Base for business-rule violations. Each subclass declares the HTTP status it maps to,
 * so the API layer needs exactly one handler instead of one per exception.
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected DomainException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
