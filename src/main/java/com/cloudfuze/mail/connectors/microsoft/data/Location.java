package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Location {
	@SerializedName("displayName")
	@Expose
	private String displayName;
	@SerializedName("locationType")
	@Expose
	private String locationType;
	@SerializedName("uniqueId")
	@Expose
	private String uniqueId;
	@SerializedName("uniqueIdType")
	@Expose
	private String uniqueIdType;
}
