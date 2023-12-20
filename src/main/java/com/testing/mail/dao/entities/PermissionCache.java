package com.testing.mail.dao.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value ="PermissionCache")
public class PermissionCache {
	@Id
	private String id;
	private String fromMail;
	private String toMail;
	private String fromCloudId;
	private String toCloudId;
	private String fromAdminCloud;
	private String toAdminCloud;
	private String userId;
	private String fromCloud;
	private String toCloud;
	private long pId;
	private boolean group;
}
