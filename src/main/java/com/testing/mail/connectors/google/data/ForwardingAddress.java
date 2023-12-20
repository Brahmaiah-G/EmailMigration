package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ForwardingAddress {
	@SerializedName("forwardingEmail")
	@Expose
	private String forwardingEmail;
	@SerializedName("verificationStatus")
	@Expose
	private String verificationStatus;
}
