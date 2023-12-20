package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class Headers {

	@Expose
	@SerializedName("Location")
	private String location;
	@Expose
	@SerializedName("Cache-Control")
	private String cacheControl;
	@Expose
	@SerializedName("OData-Version")
	private String oDataVersion;
	@Expose
	@SerializedName("Content-Type")
	private String contentType;
}
