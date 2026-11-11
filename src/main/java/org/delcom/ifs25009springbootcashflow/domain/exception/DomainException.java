package org.delcom.ifs25009springbootcashflow.domain.exception;

/** Exception dasar seluruh kesalahan bisnis (checked). */
public class DomainException extends Exception {
    public DomainException(String message) {
        super(message);
    }
}
