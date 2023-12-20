package com.cloudfuze.mail.connectors.google.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Member {

	@JsonProperty("id")
	@JsonIgnore
	private String id;
	@JsonProperty("email")
	@JsonIgnore
	private String email;
	@JsonProperty("role")
	@JsonIgnore
	private String role;
	@JsonProperty("type")
	@JsonIgnore
	private String type;
}
