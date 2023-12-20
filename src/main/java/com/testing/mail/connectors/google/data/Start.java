package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Start {
	@SerializedName("dateTime")
	@Expose
	private String dateTime;
	@SerializedName("date")
	@Expose
	private String date;
	@SerializedName("timeZone")
	@Expose
	private String timeZone;

}
