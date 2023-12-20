package com.testing.mail.connectors.google.data;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter@Getter@NoArgsConstructor
public class AccessTokenResponse implements Serializable {

	@JsonIgnore
	private static final long serialVersionUID = -5655050468862387699L;

	@JsonProperty(value = "access_token")
	private String accessToken;
	
	@JsonProperty(value = "refresh_token")
	private String refreshToken;
	
	//Expiry time in seconds
	@JsonProperty(value = "expires_in")	
	private Long expiresIn;
	
	@JsonProperty(value = "scope")
	private String scope;
	
	@JsonProperty(value = "token_type")
	private String type;
}
