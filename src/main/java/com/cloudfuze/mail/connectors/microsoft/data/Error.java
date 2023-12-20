package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Error {
	@SerializedName("code")
	@Expose
	private String code;
	@SerializedName("message")
	@Expose
	private String message;
}
