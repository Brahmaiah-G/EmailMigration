package com.cloudfuze.mail.exceptions;

public class UserSettingsException extends RuntimeException{

	private static final long serialVersionUID = -5052226039337580959L;

	public UserSettingsException() {
		super();
	}
	
	
	public static enum Error{
		pop_settings,imap_settings,vocation_settings,auto_forward_settings;
	}
	
	
	public UserSettingsException(String message) {
		super(message);
	}
	
	public UserSettingsException(Exception message) {
		super(message);
	}
	
	public UserSettingsException(String message, Throwable cause) {
		super(message, cause);
	}
}
