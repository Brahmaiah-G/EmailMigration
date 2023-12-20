package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Item {
	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("summary")
	@Expose
	private String summary;
	@SerializedName("timeZone")
	@Expose
	private String timeZone;
	@SerializedName("colorId")
	@Expose
	private String colorId;
	@SerializedName("backgroundColor")
	@Expose
	private String backgroundColor;
	@SerializedName("foregroundColor")
	@Expose
	private String foregroundColor;
	@SerializedName("selected")
	@Expose
	private Boolean selected;
	@SerializedName("accessRole")
	@Expose
	private String accessRole;
	@SerializedName("defaultReminders")
	@Expose
	private List<DefaultReminder> defaultReminders;
	@SerializedName("notificationSettings")
	@Expose
	private NotificationSettings notificationSettings;
	@SerializedName("primary")
	@Expose
	private Boolean primary;
	@SerializedName("conferenceProperties")
	@Expose
	private ConferenceProperties conferenceProperties;
}
