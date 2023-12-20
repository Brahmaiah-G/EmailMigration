package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MailValue {
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("displayName")
	@Expose
	private String displayName;
	@SerializedName("sequence")
	@Expose
	private Integer sequence;
	@SerializedName("isEnabled")
	@Expose
	private boolean isEnabled;
	@SerializedName("hasError")
	@Expose
	private boolean hasError;
	@SerializedName("isReadOnly")
	@Expose
	private boolean isReadOnly;
	@SerializedName("conditions")
	@Expose
	private Conditions conditions;
	@SerializedName("actions")
	@Expose
	private Actions actions;
}
