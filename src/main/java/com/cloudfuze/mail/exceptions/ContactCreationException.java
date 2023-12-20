package com.cloudfuze.mail.exceptions;

public class ContactCreationException extends RuntimeException {

	private static final long serialVersionUID = -8388198300234566155L;
	public ContactCreationException() {
		super();
	}

	public ContactCreationException(String message) {
		super(message);
	}

	public ContactCreationException(Exception message) {
		super(message);
	}

	public ContactCreationException(String message, Throwable cause) {
		super(message, cause);
	}

}
