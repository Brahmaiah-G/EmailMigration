package com.testing.mail.repo.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
@TypeAlias(value="MappedUsers")
public class MappedUsers {
	@Id
	String id;
	String userId;
	String fromCloudId;
	String toCloudId;
	String fromAdminCloud;
	String toAdminCloud;
	String fromCloudName;
	String toCloudName;
	boolean csv;
	boolean matched;
	String fromMailId;
	String fromMailFolder;
	String toMailFolder;
	String toMailId;
	boolean sourceVerified;
	boolean destVerified;
	boolean sourceVerifiedUser;
	boolean destVerifiedUser;
	String sourceErrorDesc;
	String destErrorDesc;
	String batchName;
	int batchId;
	boolean valid;
	int csvId;
	LocalDateTime createdTime = LocalDateTime.now();
}
