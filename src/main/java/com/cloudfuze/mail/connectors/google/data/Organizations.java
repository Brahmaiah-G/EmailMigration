package com.cloudfuze.mail.connectors.google.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Organizations {

	private String title;
	private String customType;
	private String department;
	private String description;
	private String costCenter;
	private boolean primary;
}
