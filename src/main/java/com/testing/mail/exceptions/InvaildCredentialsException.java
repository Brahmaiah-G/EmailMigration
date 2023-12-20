package com.testing.mail.exceptions;

import java.io.Serializable;

public class InvaildCredentialsException extends RuntimeException implements Serializable{
	
	private static final long serialVersionUID = 4L;

	public InvaildCredentialsException() {
		super();
	}

	public InvaildCredentialsException(String message) {
		super(message);
	}

	public InvaildCredentialsException(String message, Throwable cause) {
		super(message, cause);
	}
}