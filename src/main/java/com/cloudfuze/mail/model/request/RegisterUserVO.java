package com.cloudfuze.mail.model.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//import javax.validation.constraints.Pattern;
/**
 *  Model to Hold Cloudfuze Connect Application login request data
 */
@Getter@Setter@NoArgsConstructor
public class RegisterUserVO {
		
	@NotEmpty
	String name;
	
	@NotEmpty@Setter(AccessLevel.NONE)@Email
	String email;
	
	@NotEmpty
	String password;
	
	//@Pattern(regexp="(^([0|\\+[0-9]{1,5})?([7-9][0-9]{9})$)")
	@NotEmpty
	String phoneNumber;
	String ent;
	
	public void setEmail(String email) {		
		this.email=email.toLowerCase();
	}
}

// Aletrnate Patter for number
//  ^(?:(?:\+?1\s*(?:[.-]\s*)?)?(?:\(\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\s*\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\s*(?:[.-]\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\s*(?:[.-]\s*)?([0-9]{4})(?:\s*(?:#|x\.?|ext\.?|extension)\s*(\d+))?$
