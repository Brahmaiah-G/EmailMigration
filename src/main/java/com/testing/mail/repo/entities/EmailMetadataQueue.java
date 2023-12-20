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
@Document(value="emailMetadataQueue")
@TypeAlias(value="EmailMetadataQueue")
public class EmailMetadataQueue {

	@Id
	private String id;
	private String userId;
	private String emailWorkSpaceId;
	private String jobId;
	private PROCESS processStatus = PROCESS.IN_PROGRESS;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
	private String errorDescription;
	private CLOUD_NAME cloudName;
	
}
