package com.cloudfuze.mail.dao.entities;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserAutoForwarding {

	private boolean enabled;
	private String emailAddress;
	private String disposition;
}
