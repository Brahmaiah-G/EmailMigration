package com.testing.mail.exceptions;

public class MailMigrationException extends RuntimeException{

	private static final long serialVersionUID = -2033901835321713262L;

	public MailMigrationException() {
		super();
	}
	
	public MailMigrationException(String message) {
		super(message);
	}
	
	public MailMigrationException(Exception message) {
		super(message);
	}
	
	public MailMigrationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
