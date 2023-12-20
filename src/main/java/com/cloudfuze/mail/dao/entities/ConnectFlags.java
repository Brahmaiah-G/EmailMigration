package com.cloudfuze.mail.dao.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConnectFlags {
	private String accessToken;
	private String cloud;
	private String emailId;
	private String userId;
	private String adminCloudId;
	private String adminMemberId;
	private String nextPageToken;
	private String domain;
}
