package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Notification {
	@SerializedName("type")
	@Expose
	private String type;
	@SerializedName("method")
	@Expose
	private String method;
}
