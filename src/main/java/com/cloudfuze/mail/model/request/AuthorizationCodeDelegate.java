package com.cloudfuze.mail.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
@Setter@Getter
public class AuthorizationCodeDelegate implements Serializable {

	@JsonIgnore
	private static final long serialVersionUID = 2266746735988684715L;
	
	@JsonProperty(value = "code")@NotEmpty
	private String authCode;
}