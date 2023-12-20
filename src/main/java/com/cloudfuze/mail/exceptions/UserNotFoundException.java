package com.cloudfuze.mail.exceptions;

import java.io.Serializable;

public class UserNotFoundException extends RuntimeException implements Serializable{
	private static final long serialVersionUID = 4L;

	public UserNotFoundException() {
		super();
	}

	public UserNotFoundException(String message) {
		super(message);
	}

	public UserNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
