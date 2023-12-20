package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class Attendee {
	@SerializedName("type")
	@Expose
	private String type;
	@SerializedName("status")
	@Expose
	private Status status;
	@SerializedName("emailAddress")
	@Expose
	private EmailAddress emailAddress;
}
