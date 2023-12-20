package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Actions {
	@SerializedName("moveToFolder")
	@Expose
	private String moveToFolder;
	@SerializedName("stopProcessingRules")
	@Expose
	private boolean stopProcessingRules;
	@SerializedName("delete")
	@Expose
	private boolean delete;
	@SerializedName("copyToFolder")
	@Expose
	private String copyToFolder;
	@SerializedName("markImportance")
	@Expose
	private String markImportance;
	@SerializedName("permanentDelete")
	@Expose
	private boolean permanentDelete;
	@SerializedName("markAsRead")
	@Expose
	private boolean markAsRead;
	@SerializedName("forwardAsAttachmentTo")
	@Expose
	private List<EmailAddress> forwardAsAttachmentTo;
	@SerializedName("forwardTo")
	@Expose
	private List<ForwardTo> forwardTo;
	@SerializedName("redirectTo")
	@Expose
	private List<ForwardTo> redirectTo;
}
