package com.testing.mail.model.request;

import javax.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

@Getter
public class LoginRequestVo {
	
	@NotEmpty
	String email;
	
	@NotEmpty@Setter
	String password;
	
	private String ent;

	public void setEmail(String email) {		
		this.email=email.toLowerCase();
	}
}