package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalenderViewResponse {
	@SerializedName("response")
	@Expose
	private String response;
	@SerializedName("time")
	@Expose
	private String time;
}
