package com.cloudfuze.mail.connectors.google.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Connection {
	@JsonProperty("resourceName")
	private String resourceName;
	@JsonProperty("etag")
	private String etag;
	@JsonProperty("metadata")
	private Metadata metadata;
	@JsonProperty("names")
	private List<Name> names = null;
	@JsonProperty("coverPhotos")
	private List<CoverPhoto> coverPhotos = null;
	@JsonProperty("photos")
	private List<Photo> photos = null;
	@JsonProperty("emailAddresses")
	private List<EmailAddress> emailAddresses = null;
	@JsonProperty("phoneNumbers")
	private List<PhoneNumber> phoneNumbers = null;
	@JsonProperty("organizations")
	private List<Organization> organizations = null;
	@JsonProperty("memberships")
	private List<Membership> memberships = null;
	@JsonProperty("addresses")
	private List<Address> addresses = null;
	@JsonProperty("biographies")
	private List<Biography> biographies = null;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();
}
