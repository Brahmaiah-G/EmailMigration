package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "address",
    "primary"
})
@Setter
@Getter
public class Email {

	@JsonProperty("address")
	private String address;
	@JsonProperty("type")
	private String type;
	@JsonProperty("customType")
	private String customType;
	@JsonProperty("primary")
	private Boolean primary;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

}

