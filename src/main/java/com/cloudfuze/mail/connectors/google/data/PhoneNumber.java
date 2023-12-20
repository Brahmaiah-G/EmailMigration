package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PhoneNumber {
	@JsonProperty("metadata")
	private Metadata metadata;
	@JsonProperty("value")
	private String value;
	@JsonProperty("canonicalForm")
	private String canonicalForm;
	@JsonProperty("type")
	private String type;
	@JsonProperty("formattedType")
	private String formattedType;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
