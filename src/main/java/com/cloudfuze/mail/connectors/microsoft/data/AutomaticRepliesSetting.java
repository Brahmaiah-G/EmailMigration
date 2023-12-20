package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AutomaticRepliesSetting {
	@SerializedName("status")
	@Expose
	private String status;
	@SerializedName("externalAudience")
	@Expose
	private String externalAudience;
	@SerializedName("internalReplyMessage")
	@Expose
	private String internalReplyMessage;
	@SerializedName("externalReplyMessage")
	@Expose
	private String externalReplyMessage;
	@SerializedName("scheduledStartDateTime")
	@Expose
	private ScheduledDateTime scheduledStartDateTime;
	@SerializedName("scheduledEndDateTime")
	@Expose
	private ScheduledDateTime scheduledEndDateTime;
}
