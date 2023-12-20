package com.cloudfuze.mail.connectors.microsoft.data;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MailBoxSettings {
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("archiveFolder")
	@Expose
	private String archiveFolder;
	@SerializedName("timeZone")
	@Expose
	private String timeZone;
	@SerializedName("delegateMeetingMessageDeliveryOptions")
	@Expose
	private String delegateMeetingMessageDeliveryOptions;
	@SerializedName("dateFormat")
	@Expose
	private String dateFormat;
	@SerializedName("timeFormat")
	@Expose
	private String timeFormat;
	@SerializedName("userPurpose")
	@Expose
	private String userPurpose;
	@SerializedName("automaticRepliesSetting")
	@Expose
	private AutomaticRepliesSetting automaticRepliesSetting;
	@SerializedName("language")
	@Expose
	private Language language;
	@SerializedName("workingHours")
	@Expose
	private WorkingHours workingHours;
}
