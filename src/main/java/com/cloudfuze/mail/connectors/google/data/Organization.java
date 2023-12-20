package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Organization {
	@JsonProperty("metadata")
	private Metadata metadata;
	@JsonProperty("name")
	private String name;
	@JsonProperty("title")
	private String title;
	@JsonProperty("department")
	private String department;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
