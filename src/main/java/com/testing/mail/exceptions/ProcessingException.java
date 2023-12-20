package com.testing.mail.exceptions;

public class ProcessingException extends RuntimeException {

	private static final long serialVersionUID = 3898279796058926416L;

	public ProcessingException() {
		super();
	}

	public ProcessingException(String message) {
		super(message);
	}

	public ProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

}
