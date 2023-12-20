package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Delegate {

	@SerializedName("delegateEmail")
	@Expose
	private String delegateEmail;
	@SerializedName("verificationStatus")
	@Expose
	private String verificationStatus;

}
