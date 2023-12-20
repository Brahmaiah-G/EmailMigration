package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Membership {
	@JsonProperty("metadata")
	private Metadata metadata;
	@JsonProperty("contactGroupMembership")
	private ContactGroupMembership contactGroupMembership;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
