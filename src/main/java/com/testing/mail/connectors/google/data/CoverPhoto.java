package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CoverPhoto {
	@JsonProperty("metadata")
	private Metadata metadata;
	@JsonProperty("url")
	private String url;
	@JsonProperty("default")
	private Boolean _default;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
