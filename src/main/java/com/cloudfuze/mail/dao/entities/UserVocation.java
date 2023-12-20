package com.cloudfuze.mail.dao.entities;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserVocation {
	private boolean enableAutoReply;
	private String responseSubject;
	private String responseBodyPlainText;
	private String responseBodyHtml;
	private boolean restrictToContacts;
	private boolean restrictToDomain;
	private String startTime;
	private String endTime;
}
