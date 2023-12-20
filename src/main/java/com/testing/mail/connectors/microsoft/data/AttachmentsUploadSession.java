package com.testing.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AttachmentsUploadSession {
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("uploadUrl")
	@Expose
	private String uploadUrl;
	@SerializedName("expirationDateTime")
	@Expose
	private String expirationDateTime;
	@SerializedName("nextExpectedRanges")
	@Expose
	private String[] nextExpectedRanges;
}
