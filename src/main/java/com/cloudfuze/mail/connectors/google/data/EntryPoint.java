package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EntryPoint {
	@SerializedName("entryPointType")
	@Expose
	private String entryPointType;
	@SerializedName("uri")
	@Expose
	private String uri;
	@SerializedName("label")
	@Expose
	private String label;
	@SerializedName("pin")
	@Expose
	private String pin;
	@SerializedName("regionCode")
	@Expose
	private String regionCode;
}
