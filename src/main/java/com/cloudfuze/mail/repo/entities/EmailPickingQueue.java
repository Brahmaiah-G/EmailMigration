package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(value="EmailPikingQueue")
@TypeAlias(value="EmailPikingQueue")
public class EmailPickingQueue {

	private String id;
	private String userId;
	private String emailWorkSpaceId;
	private String jobId;
	private PROCESS processStatus = PROCESS.NOT_STARTED;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String errorDescription;
	private CLOUD_NAME cloudName;
	private boolean mailRules;
	private boolean contacts;
	private boolean mails;
	private boolean settings;
}
