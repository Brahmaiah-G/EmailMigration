package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Criteria {
	@SerializedName("from")
	@Expose
	private String from;
	@SerializedName("to")
	@Expose
	private String to;
	@SerializedName("subject")
	@Expose
	private String subject;
	@SerializedName("query")
	@Expose
	private String query;
	@SerializedName("negatedQuery")
	@Expose
	private String negatedQuery;
	@SerializedName("hasAttachment")
	@Expose
	private boolean hasAttachment;
	@SerializedName("excludeChats")
	@Expose
	private boolean excludeChats;
	@SerializedName("size")
	@Expose
	private long size;
	@SerializedName("sizeComparison")
	@Expose
	private String sizeComparison;
}
