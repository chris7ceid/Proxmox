package org.ceid_uni.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidRoleException extends AuthenticationException {
    public InvalidRoleException(String message) {
        super(message);
    }
}
