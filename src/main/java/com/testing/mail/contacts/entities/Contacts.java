package com.testing.mail.contacts.entities;

import java.util.List;

import lombok.Data;

@Data
public class Contacts {
	private String id;
	private String firstName;
	private String lastName;
	private List<Emails> emailAddresses;
	private List<PhoneNumbers> phoneNumbers;
	private String notes;
	private String jobTitle;
	private String companyName;
	private String department;
	private List<Address> address;
}
