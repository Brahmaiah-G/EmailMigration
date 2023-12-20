package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SendAs {
	@SerializedName("sendAsEmail")
	@Expose
	private String sendAsEmail;
	@SerializedName("displayName")
	@Expose
	private String displayName;
	@SerializedName("replyToAddress")
	@Expose
	private String replyToAddress;
	@SerializedName("signature")
	@Expose
	private String signature;
	@SerializedName("isPrimary")
	@Expose
	private boolean isPrimary;
	@SerializedName("isDefault")
	@Expose
	private boolean isDefault;
	@SerializedName("verificationStatus")
	@Expose
	private String verificationStatus;
	@SerializedName("treatAsAlias")
	@Expose
	private boolean treatAsAlias;
}
