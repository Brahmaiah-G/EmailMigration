package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Attachment {

	@SerializedName("fileUrl")
	@Expose
	private String fileUrl;
	@SerializedName("title")
	@Expose
	private String title;
	@SerializedName("mimeType")
	@Expose
	private String mimeType;
	@SerializedName("iconLink")
	@Expose
	private String iconLink;
	@SerializedName("fileId")
	@Expose
	private String fileId;
}
