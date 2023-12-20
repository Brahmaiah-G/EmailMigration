package com.cloudfuze.mail.dao.entities;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserGroups {
	private String id;
	private String email;
	private String name;
	private String description;
	private List<String> members;
	private LocalDateTime createdTime;
	private long membersCount;
	private long ownersCount;
	private String errorDescription;
	private List<String> types;
}
