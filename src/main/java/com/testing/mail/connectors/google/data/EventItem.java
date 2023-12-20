package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EventItem {
	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("status")
	@Expose
	private String status;
	@SerializedName("htmlLink")
	@Expose
	private String htmlLink;
	@SerializedName("created")
	@Expose
	private String created;
	@SerializedName("updated")
	@Expose
	private String updated;
	@SerializedName("summary")
	@Expose
	private String summary;
	@SerializedName("creator")
	@Expose
	private Creator creator;
	@SerializedName("organizer")
	@Expose
	private Organizer organizer;
	@SerializedName("start")
	@Expose
	private Start start;
	@SerializedName("end")
	@Expose
	private End end;
	@SerializedName("iCalUID")
	@Expose
	private String iCalUID;
	@SerializedName("sequence")
	@Expose
	private Integer sequence;
	@SerializedName("attendees")
	@Expose
	private List<Attendee> attendees;
	@SerializedName("hangoutLink")
	@Expose
	private String hangoutLink;
	@SerializedName("conferenceData")
	@Expose
	private ConferenceData conferenceData;
	@SerializedName("reminders")
	@Expose
	private Reminders reminders;
	@SerializedName("eventType")
	@Expose
	private String eventType;
	@SerializedName("description")
	@Expose
	private String description;
	@SerializedName("recurrence")
	@Expose
	private List<String> recurrence;
	@SerializedName("location")
	@Expose
	private String location;
	@SerializedName("attachments")
	@Expose
	private List<Attachment>attachments;
	@SerializedName("colorId")
	@Expose
	private String colorId;
	@SerializedName("recurringEventId")
	@Expose
	private String recurringEventId;
	
}
