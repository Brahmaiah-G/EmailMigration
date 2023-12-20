package com.cloudfuze.mail.contacts.dao;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ContactsFlagInfo {
	private String cloudId;
	private String name;
	private List<String>emailAddress;
	private String id;
	private List<String>phoneNumbers;
	private String profilePic;
	private String businessName;
	private String notes;
	private String nextPageToken;
}
