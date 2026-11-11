package org.delcom.ifs25009springbootcashflow.adapter.repository;

/** Kegagalan teknis penyimpanan (mis. SQLException) yang dibungkus agar domain tetap bersih. */
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
