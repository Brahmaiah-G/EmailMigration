package com.testing.mail.dao.entities;

import java.util.List;

import com.testing.mail.connectors.microsoft.data.AttachmentsData;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CalenderFlags {

	String cloudId;
	String organizer;
	List<String> attendees;
	List<AttachmentsData>attachments;
	String subject;
	String message;
	String createdTime;
	boolean htmlContent;
	boolean metaData;
	String htmlMessage;
	String threadId;
	String id;
	boolean flagged;
	boolean remainderOn;
	long sizeInBytes;
	String parentFolderId;
	String sentTime;
	boolean hadAttachments;
	String name;
	String destParent;
	String userId;
	String emailId;
	String calendar;
	String bodyPreview;
	String importance;
	String nextPageToken;
	boolean externalUser;
	boolean onlineMeeting;
	boolean deleted;
	String destId;
	String fromName;
	String startTime;
	String endTime;
	String timeZone;
	String endTimeZone;
	boolean folder;
	String onlineMeetingUrl; 
	String recurrenceType;
	String range;
	List<String> location;
	String colour;
	private boolean largeFile;
	private boolean externalOrg;
	private String iCalUId;
	private String remainders;
	private String visibility;
	private boolean futureEvents;
	private String syncToken;
	private String lastOccurence;
}
