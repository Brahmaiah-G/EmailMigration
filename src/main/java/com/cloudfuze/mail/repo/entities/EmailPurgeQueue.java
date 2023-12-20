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
@Document(value="emailPurgeQueue")
@TypeAlias(value="EmailPurgeQueue")
public class EmailPurgeQueue {

	@Id
	private String id;
	private String userId;
	private String emailWorkSpaceId;
	private String jobId;
	private PROCESS processStatus = PROCESS.NOT_PROCESSED;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String errorDescription;
	public CLOUD_NAME cloudName;
	
}
