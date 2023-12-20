package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AutoForwardingSettings {

	@SerializedName("enableAutoReply")
	@Expose
	private Boolean enableAutoReply;
	@SerializedName("responseSubject")
	@Expose
	private String responseSubject;
	@SerializedName("restrictToContacts")
	@Expose
	private Boolean restrictToContacts;
	@SerializedName("restrictToDomain")
	@Expose
	private Boolean restrictToDomain;

}
