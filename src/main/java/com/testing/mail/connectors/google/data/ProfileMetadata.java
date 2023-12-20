package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ProfileMetadata {
	@JsonProperty("objectType")
	private String objectType;
	@JsonProperty("userTypes")
	private List<String> userTypes = null;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();
}
