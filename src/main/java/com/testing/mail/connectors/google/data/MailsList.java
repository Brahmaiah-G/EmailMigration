package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MailsList {
	@SerializedName("resultSizeEstimate")
	@Expose
	private Integer resultSizeEstimate;
	@SerializedName("messages")
	@Expose
	private List<Message> messages;
	@SerializedName("nextPageToken")
	@Expose
	private String nextPageToken;
}
