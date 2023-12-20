package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EventsList {

	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("summary")
	@Expose
	private String summary;
	@SerializedName("updated")
	@Expose
	private String updated;
	@SerializedName("timeZone")
	@Expose
	private String timeZone;
	@SerializedName("accessRole")
	@Expose
	private String accessRole;
	@SerializedName("defaultReminders")
	@Expose
	private List<DefaultReminder> defaultReminders;
	@SerializedName("nextPageToken")
	@Expose
	private String nextPageToken;
	@SerializedName("items")
	@Expose
	private List<EventItem> items;
	@SerializedName("nextSyncToken")
	@Expose
	private String nextSyncToken;
}
