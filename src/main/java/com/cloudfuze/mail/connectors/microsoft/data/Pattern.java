package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Pattern {

	@SerializedName("type")
	@Expose
	private String type;
	@SerializedName("interval")
	@Expose
	private Integer interval;
	@SerializedName("month")
	@Expose
	private Integer month;
	@SerializedName("dayOfMonth")
	@Expose
	private Integer dayOfMonth;
	@SerializedName("firstDayOfWeek")
	@Expose
	private String firstDayOfWeek;
	@SerializedName("index")
	@Expose
	private String index;
	@SerializedName("daysOfWeek")
	@Expose
	List<String>daysOfWeek;

}
