package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ThreadMessages {
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("historyId")
	@Expose
	private String historyId;
	@SerializedName("messages")
	@Expose
	private List<MailValue> messages;
}
