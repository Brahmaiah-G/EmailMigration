package com.cloudfuze.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias("EmailBatches")
public class EmailBatches {

	@Id
	private String id;
	private String userId;
	private String fromCloudId;
	private String toCloudId;
	private String batchName;
	private String moveWorkSpaceId;
	private int batchId;
	private LocalDateTime createdTime = LocalDateTime.now();
	private String fromAdminCloud;
	private String toAdminCloud;
	private String fromMailId;
	private String toMailId;
}
