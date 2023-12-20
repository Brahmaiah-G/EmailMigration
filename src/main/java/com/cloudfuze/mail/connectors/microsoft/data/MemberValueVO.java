package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter@Setter@ToString
@JsonInclude(value=Include.NON_EMPTY, content=Include.NON_NULL)

@JsonIgnoreProperties({"@odata.context","@odata.type"})
public class MemberValueVO {

	@JsonProperty("@odata.context")
	@JsonIgnore
	private String odataContext;
	@JsonProperty("id")
	private String id;
	@JsonProperty("businessPhones")
	private List<Object> businessPhones = new ArrayList<Object>();
	@JsonProperty("displayName")
	private String displayName;
	@JsonProperty("givenName")
	private String givenName;
	@JsonProperty("jobTitle")
	private Object jobTitle;
	@JsonProperty("mail")
	private String mail;
	@JsonProperty("mobilePhone")
	private Object mobilePhone;
	@JsonProperty("officeLocation")
	private Object officeLocation;
	@JsonProperty("preferredLanguage")
	private Object preferredLanguage;
	@JsonProperty("surname")
	private Object surname;
	@JsonProperty("userPrincipalName")
	private String userPrincipalName;
	@JsonProperty("signInSessionsValidFromDateTime")
	@JsonIgnore
	private String signInSessionsValidFromDateTime;
	@JsonProperty("proxyAddresses")
	@JsonIgnore
	private List<String>proxyAddresses;
}

