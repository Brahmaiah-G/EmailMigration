package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Address {
	@JsonProperty("metadata")
	private Metadata metadata;
	@JsonProperty("formattedValue")
	private String formattedValue;
	@JsonProperty("streetAddress")
	private String streetAddress;
	@JsonProperty("extendedAddress")
	private String extendedAddress;
	@JsonProperty("city")
	private String city;
	@JsonProperty("region")
	private String region;
	@JsonProperty("postalCode")
	private String postalCode;
	@JsonProperty("country")
	private String country;
	@JsonProperty("countryCode")
	private String countryCode;
	@JsonProperty("type")
	private String type;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();
}
