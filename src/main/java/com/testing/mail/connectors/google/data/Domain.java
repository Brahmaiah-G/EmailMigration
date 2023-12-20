package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Domain {
	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("domainName")
	@Expose
	private String domainName;
	@SerializedName("domainAliases")
	@Expose
	private List<DomainAlias> domainAliases;
	@SerializedName("isPrimary")
	@Expose
	private Boolean isPrimary;
	@SerializedName("verified")
	@Expose
	private Boolean verified;
	@SerializedName("creationTime")
	@Expose
	private String creationTime;
}
