package com.cloudfuze.mail.exceptions;

public class TokenExpiredException extends RuntimeException {

	private static final long serialVersionUID = 6677181022952883646L;

	public TokenExpiredException() {
		super();
	}

	public TokenExpiredException(String message) {
		super(message);
	}

	public TokenExpiredException(String message, Throwable cause) {
		super(message, cause);
	}
}
