package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter@Getter@ToString
@JsonPropertyOrder({
"expires_in",
"token_type",
"scope",
"expires_on",
"not_before",
"resource",
"access_token",
"refresh_token",
"ext_expires_in"
})
public class RefreshTokenResult {

	@JsonProperty("expires_in")
	private String expiresIn;
	@JsonProperty("token_type")
	private String tokenType;
	@JsonProperty("scope")
	private String scope;
	@JsonProperty("expires_on")
	private String expiresOn;
	@JsonProperty("not_before")
	private String notBefore;
	@JsonProperty("resource")
	private String resource;
	@JsonProperty("access_token")
	private String accessToken;
	@JsonProperty("refresh_token")
	private String refreshToken;
	
	@JsonProperty("ext_expires_in")
	private String ext_expires_in;
//	Properties set when token request returns error
	@JsonProperty("error")
	private String error;
	@JsonProperty("error_description")	
	private String errorDescription;
	@JsonProperty("timestamp")	
	private String timestamp;
	@JsonProperty("trace_id")	
	private String traceId;
	@JsonProperty("correlation_id")	
	private String correlationId;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();


}
