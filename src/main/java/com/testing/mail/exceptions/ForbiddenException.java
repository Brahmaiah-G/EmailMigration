package com.testing.mail.exceptions;

public class ForbiddenException extends RuntimeException{

	private static final long serialVersionUID = 797184135583557873L;

	public ForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

	public ForbiddenException(String message) {
		super(message);
	}

}
