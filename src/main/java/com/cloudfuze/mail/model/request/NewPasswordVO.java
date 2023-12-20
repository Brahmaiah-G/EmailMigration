package com.cloudfuze.mail.model.request;

import javax.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class NewPasswordVO {

	@NotEmpty
	String orginalPassword;
	
	@NotEmpty
	String newPassword;

}