package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DomainAlias {
	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("domainAliasName")
	@Expose
	private String domainAliasName;
	@SerializedName("parentDomainName")
	@Expose
	private String parentDomainName;
	@SerializedName("verified")
	@Expose
	private Boolean verified;
	@SerializedName("creationTime")
	@Expose
	private String creationTime;
}
