package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WorkingHours {
	@SerializedName("daysOfWeek")
	@Expose
	private List<String> daysOfWeek;
	@SerializedName("startTime")
	@Expose
	private String startTime;
	@SerializedName("endTime")
	@Expose
	private String endTime;
	@SerializedName("timeZone")
	@Expose
	private TimeZone timeZone;
}
