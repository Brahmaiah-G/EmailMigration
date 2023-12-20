package com.testing.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(value="premigrationDetails")
@TypeAlias("premigrationDetails")
public class PremigrationDetails {
	@Id
	private String id;
	private String userId;
	private PROCESS processStatus;
	private long totalMails;
	private long totalFolders;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String cloudId;
	private CLOUD_NAME cloudName;
	private String email;
	private long totalCalendarInvites;
	private String emailWorkSpaceId;
	private long totalSize;
}
