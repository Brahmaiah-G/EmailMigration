package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ContactInfo {
	@JsonProperty("connections")
	private List<Connection> connections = null;
	@JsonProperty("totalPeople")
	private Integer totalPeople;
	@JsonProperty("totalItems")
	private Integer totalItems;
	@JsonProperty("nextPageToken")
	private String nextPageToken;
	@JsonProperty("nextLink")
	private String nextLink;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
