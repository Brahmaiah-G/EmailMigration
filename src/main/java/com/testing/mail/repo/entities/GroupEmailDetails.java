package com.testing.mail.repo.entities;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
@TypeAlias(value="groups")
public class GroupEmailDetails {
	@Id
	private String id;
	private String userId;
	private String email;
	private String name;
	private String description;
	private String groupId;
	private List<String> members;
	private Date createdTime;
	private String errorDescription;
	private String adminCloudId;
	private PROCESS processStatus = PROCESS.NOT_PROCESSED;
}
