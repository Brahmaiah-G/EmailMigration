package com.testing.mail.contacts.entities;

import lombok.Data;

@Data
public class Address {
	private String street;
	private String city;
	private String state;
	private String countryOrRegion;
	private String postalCode;
	private String addressType;
}
