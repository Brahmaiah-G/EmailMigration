package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Source {
	@JsonProperty("type")
	private String type;
	@JsonProperty("id")
	private String id;
	@JsonProperty("etag")
	private String etag;
	@JsonProperty("updateTime")
	private String updateTime;
	@JsonProperty("profileMetadata")
	private ProfileMetadata profileMetadata;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();

}
