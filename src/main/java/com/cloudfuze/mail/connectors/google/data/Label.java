package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Label {

	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("messageListVisibility")
	@Expose
	private String messageListVisibility;
	@SerializedName("labelListVisibility")
	@Expose
	private String labelListVisibility;
	@SerializedName("type")
	@Expose
	private String type;


}
