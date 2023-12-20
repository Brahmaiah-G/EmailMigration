package com.cloudfuze.mail.connectors.google.data;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter@Getter@NoArgsConstructor
@JsonInclude(content = Include.NON_EMPTY)
public class AccessTokenRequest implements Serializable {

	@JsonIgnore
	private static final long serialVersionUID = 910342337234163940L;
	
	@JsonProperty(value = "code")
	private String authCode;
	
	@JsonProperty(value = "client_id")
	private String 	clientId;
	
	@JsonProperty(value = "client_secret")
	private String  clientSecret;
	
	@JsonProperty(value = "redirect_uri")
	private String redirectURL;
	
	@JsonProperty(value = "grant_type")
	private String grant_type;
	
	@JsonProperty(value = "refresh_token")
	private String  refreshToken;

}
