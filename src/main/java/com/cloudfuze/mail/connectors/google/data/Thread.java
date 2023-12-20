package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;


@Data
public class Thread {
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("snippet")
	@Expose
	private String snippet;
	@SerializedName("historyId")
	@Expose
	private String historyId;
}
