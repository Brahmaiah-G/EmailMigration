package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(value="mailChangesQueue")
@TypeAlias(value="MailChangesQueue")
public class MailChangesQueue {

	@Id
	private String id;
	private String userId;
	private String emailWorkSpaceId;
	private String jobId;
	private PROCESS processStatus = PROCESS.IN_PROGRESS;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String errorDescription;
	public CLOUD_NAME cloudName;
	
}
