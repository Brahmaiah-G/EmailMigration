package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MailValue {
	@SerializedName("internalDate")
	@Expose
	private String internalDate;
	@SerializedName("historyId")
	@Expose
	private String historyId;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("snippet")
	@Expose
	private String snippet;
	@SerializedName("sizeEstimate")
	@Expose
	private Integer sizeEstimate;
	@SerializedName("threadId")
	@Expose
	private String threadId;
	@SerializedName("labelIds")
	@Expose
	private List<String> labelIds;
	@SerializedName("payload")
	@Expose
	private Payload payload;
}
