package com.testing.mail.connectors.impl.helper;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.testing.mail.contacts.entities.Contacts;
import com.testing.mail.contacts.entities.Emails;
import com.testing.mail.contacts.entities.PhoneNumbers;

import net.minidev.json.JSONArray;

@Service
public class ContactsHelper {

	/**
	 * Body For Creating Contact In <b>OUTLOOK</b>
	 * @param contacts : contactsBody for creating a Contact
	*/
	public JSONObject createBodyForContact(Contacts contacts) {
		JSONObject body = new JSONObject();
		body.put("givenName", contacts.getFirstName());
		body.put("surname", contacts.getLastName());
		JSONArray emails = new JSONArray();
		for(Emails email: contacts.getEmailAddresses()) {
			JSONObject emailAddress = new JSONObject();
			emailAddress.put("address", email.getEmailAddress());
			emailAddress.put("name", email.getName());
			emails.add(emailAddress);
		}
		JSONArray phoneNumber = new JSONArray();
		body.put("emailAddresses", emails);
		if(null!=contacts.getPhoneNumbers()) {
			for(PhoneNumbers number : contacts.getPhoneNumbers()) {
				phoneNumber.add(number.getPhoneNo());
			}
		}
		body.put("businessPhones", phoneNumber);
		body.put("personalNotes", contacts.getNotes());
		return body;
	}
	
}
