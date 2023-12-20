package com.testing.mail.repo.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.testing.mail.dao.entities.Enums.Status;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value="clouds")
public class Clouds {

	@Id
	private String id;
	private CLOUD_NAME cloudName;
	
	public enum CLOUD_NAME{
		GMAIL,OUTLOOK;
	}
	private String userId;
	private String email;
	private Status status;
	private String adminEmailId;
	private String domain;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String memberId;
	private String name;
	private String metaUrl;
	private boolean admin;
	private boolean active;
	private String adminCloudId;
	private String adminMemberId;
	private boolean picking;
	private boolean reAuthenticate;
	private long total;
	private long provisioned;
	private long nonProvisioned;
	private List<String> smtps;
	private String deltaChangeId;
	private String nextPageToken;
	private String errorDescription;
	private  String timeZone;
	private boolean calendarEnabled=true;
	List<String>domains;
	private boolean exchange;
	private String driveId;
	@JsonIgnore
	@DBRef
	private VendorOAuthCredential credential;
	private String preMigrationStatus;
	private boolean rateLimit;
	private long rCount;
	public enum RTYPE{
		HOUR,DAY,MINUTE;
	}
	private RTYPE rType = RTYPE.HOUR;
	private String mailBoxStatus;
	private PROCESS groupsStatus;
	private long quotaBytesUsed;
	private String quotaUsed;
	private String totalUsersQuotaUsed;
	
}
