package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Metadata {
	@JsonProperty("sources")
	private List<Source> sources = null;
	@JsonProperty("objectType")
	private String objectType;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();
}
