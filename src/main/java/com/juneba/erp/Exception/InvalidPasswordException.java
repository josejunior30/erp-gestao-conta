package com.juneba.erp.Exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidPasswordException extends AuthenticationException {
	private static final long serialVersionUID = 1L;

	public InvalidPasswordException() {
		super("Senha incorreta");
	}

	public InvalidPasswordException(String msg) {
		super(msg);
	}
}