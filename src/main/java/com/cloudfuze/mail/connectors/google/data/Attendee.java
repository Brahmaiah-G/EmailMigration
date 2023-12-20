package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Attendee {
	@SerializedName("email")
	@Expose
	private String email;
	@SerializedName("responseStatus")
	@Expose
	private String responseStatus;
	@SerializedName("organizer")
	@Expose
	private Boolean organizer;
	@SerializedName("self")
	@Expose
	private Boolean self;
	@SerializedName("resource")
	@Expose
	private boolean resource;
	@SerializedName("optional")
	@Expose
	private boolean optional;
	@SerializedName("comment")
	@Expose
	private String comment;
}
