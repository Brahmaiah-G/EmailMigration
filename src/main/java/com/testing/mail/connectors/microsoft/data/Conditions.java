package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Conditions {
	@SerializedName("sentToAddresses")
	@Expose
	private List<SentToAddress> sentToAddresses;
	@SerializedName("fromAddresses")
	@Expose
	private List<FromAddress> fromAddresses;
}
