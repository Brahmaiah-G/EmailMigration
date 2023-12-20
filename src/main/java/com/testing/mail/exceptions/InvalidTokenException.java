package com.testing.mail.exceptions;

public class InvalidTokenException extends RuntimeException{

	private static final long serialVersionUID = 8354517579018558108L;

	public InvalidTokenException() {}
	
	public InvalidTokenException(String message) {
		super(message);
	}
	
	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}
	

}
