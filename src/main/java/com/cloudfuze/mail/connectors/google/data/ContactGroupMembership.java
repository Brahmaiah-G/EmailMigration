package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ContactGroupMembership {
	@JsonProperty("contactGroupId")
	private String contactGroupId;
	@JsonProperty("contactGroupResourceName")
	private String contactGroupResourceName;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
