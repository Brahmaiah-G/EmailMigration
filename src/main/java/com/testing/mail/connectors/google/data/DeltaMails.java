package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class DeltaMails {

	@SerializedName("history")
	@Expose
	private List<History> history;
	@SerializedName("nextPageToken")
	@Expose
	private String nextPageToken;
	@SerializedName("historyId")
	@Expose
	private String historyId;
}
