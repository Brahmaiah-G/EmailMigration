package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalenderViewState {
	@SerializedName("dateTime")
	@Expose
	private String dateTime;
	@SerializedName("timeZone")
	@Expose
	private String timeZone;
}
