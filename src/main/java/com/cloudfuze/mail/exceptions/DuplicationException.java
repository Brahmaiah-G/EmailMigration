package com.cloudfuze.mail.exceptions;

import java.io.Serializable;

public class DuplicationException extends RuntimeException implements Serializable{

	private static final long serialVersionUID = 7364067749994658973L;
	
	public DuplicationException() {}
	
	public DuplicationException(String message) {
		super(message);
	}

	public DuplicationException(String message, Throwable cause) {
		super(message, cause);
	}

}
