package com.testing.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Range {

	@SerializedName("type")
	@Expose
	private String type;
	@SerializedName("startDate")
	@Expose
	private String startDate;
	@SerializedName("endDate")
	@Expose
	private String endDate;
	@SerializedName("recurrenceTimeZone")
	@Expose
	private String recurrenceTimeZone;
	@SerializedName("numberOfOccurrences")
	@Expose
	private Integer numberOfOccurrences;

}
