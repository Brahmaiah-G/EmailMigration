package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalenderViewValue {
	@SerializedName("@odata.etag")
	@Expose
	private String odataEtag;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("createdDateTime")
	@Expose
	private String createdDateTime;
	@SerializedName("lastModifiedDateTime")
	@Expose
	private String lastModifiedDateTime;
	@SerializedName("changeKey")
	@Expose
	private String changeKey;
	@SerializedName("categories")
	@Expose
	private List<String> categories;
	@SerializedName("transactionId")
	@Expose
	private Object transactionId;
	@SerializedName("originalStartTimeZone")
	@Expose
	private String originalStartTimeZone;
	@SerializedName("originalEndTimeZone")
	@Expose
	private String originalEndTimeZone;
	@SerializedName("uid")
	@Expose
	private String uid;
	@SerializedName("reminderMinutesBeforeStart")
	@Expose
	private Integer reminderMinutesBeforeStart;
	@SerializedName("isReminderOn")
	@Expose
	private Boolean isReminderOn;
	@SerializedName("hasAttachments")
	@Expose
	private Boolean hasAttachments;
	@SerializedName("subject")
	@Expose
	private String subject;
	@SerializedName("bodyPreview")
	@Expose
	private String bodyPreview;
	@SerializedName("importance")
	@Expose
	private String importance;
	@SerializedName("sensitivity")
	@Expose
	private String sensitivity;
	@SerializedName("isAllDay")
	@Expose
	private Boolean isAllDay;
	@SerializedName("isCancelled")
	@Expose
	private Boolean isCancelled;
	@SerializedName("isOrganizer")
	@Expose
	private Boolean isOrganizer;
	@SerializedName("responseRequested")
	@Expose
	private Boolean responseRequested;
	@SerializedName("seriesMasterId")
	@Expose
	private String seriesMasterId;
	@SerializedName("showAs")
	@Expose
	private String showAs;
	@SerializedName("type")
	@Expose
	private String type;
	@SerializedName("webLink")
	@Expose
	private String webLink;
	@SerializedName("onlineMeetingUrl")
	@Expose
	private Object onlineMeetingUrl;
	@SerializedName("isOnlineMeeting")
	@Expose
	private Boolean isOnlineMeeting;
	@SerializedName("onlineMeetingProvider")
	@Expose
	private String onlineMeetingProvider;
	@SerializedName("allowNewTimeProposals")
	@Expose
	private Boolean allowNewTimeProposals;
	@SerializedName("occurrenceId")
	@Expose
	private String occurrenceId;
	@SerializedName("isDraft")
	@Expose
	private Boolean isDraft;
	@SerializedName("hideAttendees")
	@Expose
	private Boolean hideAttendees;
	@SerializedName("responseStatus")
	@Expose
	private Status responseStatus;
	@SerializedName("body")
	@Expose
	private Body body;
	@SerializedName("start")
	@Expose
	private CalenderViewState start;
	@SerializedName("end")
	@Expose
	private CalenderViewState end;
	@SerializedName("locations")
	@Expose
	private List<Location> locations;
	@SerializedName("recurrence")
	@Expose
	private Recurrence recurrence;
	@SerializedName("attendees")
	@Expose
	private List<Attendee> attendees;
	@SerializedName("organizer")
	@Expose
	private Organizer organizer;
	@SerializedName("onlineMeeting")
	@Expose
	private OnlineMeeting onlineMeeting;
	@SerializedName("calendar@odata.associationLink")
	@Expose
	private String calendarOdataAssociationLink;
	@SerializedName("iCalUId")
	@Expose
	private String iCalUId;
}
