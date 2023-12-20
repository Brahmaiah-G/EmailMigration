package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;


/**
 * For storing calendarInstanceEvents info
*/


@Setter
@Getter
@Document
@TypeAlias(value="eventInstacesInfo")
public class EventInstacesInfo {

	@Id
	private String id;
	private String emailWorkSpaceId;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String calenderCreatedTime;
	private String calenderModifiedTime;
	private String organizer;
	private List<String> attendees;
	private boolean attachMents;
	private String userId;
	private PROCESS processStatus = PROCESS.NOT_PROCESSED;
	private String sourceId;
	private String destId;
	private String threadId;
	private boolean calender;
	private boolean metadata;
	private long retryCount;
	private String errorDescription;
	private String threadBy=null;
	private String subject;
	private boolean htmlContent;
	private boolean primaryCalender;
	private String bodyPreview;
	private String htmlBodyContent;
	private String destThreadId;
	private String destFolderPath;
	private String fromCloudId;
	private String toCloudId;
	private long attachmentsSize;
	private boolean preScan;
	private String sourceParent;
	private String destParent;
	private boolean created;
	private boolean picking;
	private String jobId;
	private String updatedMetadata;
	private boolean draft;
	private String nextPageToken;
	private boolean externalOrganizer;
	private String originalFrom;
	private List<String> attachmentIds = new ArrayList<>();
	private boolean reminderOn;
	private boolean deleted;
	private String parentName;
	private boolean flagged;
	private List<String>categories;
	private String fromName;
	private String uid;
	private long remainderTime;
	private boolean allDay;
	private String type;
	private boolean onlineMeeting;
	private String onlineMeetingUrl;
	private String startTime;
	private String endTime;
	private String timeZone;
	private String endTimeZone;
	private String recurrenceType;
	private String range ;
	private List<String> location;
	private long count;
	private boolean readOnly;
	private String color;
	private String env;
	private boolean largeFile;
	private String iCalUId;
	private String remainders;
	private String showAs;
	private String visibility;
	private boolean attendeeEventDeleted;
	private String recurenceId;
	private List<String>emdAtendee;
	private boolean parent = false;
	
}
