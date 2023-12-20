package com.testing.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SentToAddress {
	@SerializedName("emailAddress")
	@Expose
	private EmailAddress emailAddress;
}
