package com.testing.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmailAttachMentValue {

	@SerializedName("@odata.type")
	@Expose
	private String odataType;
	@SerializedName("@odata.mediaContentType")
	@Expose
	private String odataMediaContentType;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("lastModifiedDateTime")
	@Expose
	private String lastModifiedDateTime;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("contentType")
	@Expose
	private String contentType;
	@SerializedName("size")
	@Expose
	private Integer size;
	@SerializedName("isInline")
	@Expose
	private Boolean isInline;
	@SerializedName("contentId")
	@Expose
	private Object contentId;
	@SerializedName("contentLocation")
	@Expose
	private Object contentLocation;
	@SerializedName("contentBytes")
	@Expose
	private String contentBytes;

}
