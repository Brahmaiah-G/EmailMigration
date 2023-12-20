package com.cloudfuze.mail.exceptions;

import java.io.Serializable;

/**
 * Custom exception handler
*/
public class MailCreationException extends RuntimeException implements Serializable{
	private static final long serialVersionUID = 4L;

	public MailCreationException() {
		super();
	}

	public MailCreationException(String message) {
		super(message);
	}

	public MailCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
