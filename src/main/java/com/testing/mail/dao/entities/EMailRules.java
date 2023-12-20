package com.testing.mail.dao.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.testing.mail.repo.entities.PROCESS;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value = "EmailRules")
public class EMailRules {

	@Id
	private String id;
	private String sourceId;
	private String emailWorkSpaceId;
	private LocalDateTime createdTime;
	private String cloudId;
	private LocalDateTime modifiedTime;
	private List<String>sentToAddresses;
	private boolean enabled;
	private List<String>fromAddresses;
	private String action;
	private List<String> mailFolder;
	private String displayName;
	private String userId;
	private boolean delete;
	private boolean markAsRead;
	private boolean markImportance;
	private boolean flagged;
	private String destId;
	private boolean attachments;
	private long minimumSize;
	private long maximumSize;
	private String query;
	private boolean negotiation;
	private List<String>forwards;
	private List<String>redirectTo;
	private String errorDescription;
	private List<String>removeLables;
	private PROCESS processStatus;
	private String customFolder;
	private String subjectContains;
	private String negatedQuery;
	private String mailFolderName;
	private String threadBy;
}
