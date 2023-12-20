package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class MailThreads {

	@SerializedName("threads")
	@Expose
	private List<Thread> threads;
	@SerializedName("resultSizeEstimate")
	@Expose
	private Integer resultSizeEstimate;
	@SerializedName("nextPageToken")
	@Expose
	private String nextPageToken;
}
