package com.cloudfuze.mail.dao.entities;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserImap {
	private boolean enabled;
	private boolean autoExpunge;
	private String expungeBehavior;
	private long maxFolderSize;
}
