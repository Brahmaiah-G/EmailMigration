package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenResponse {

	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("nextSyncToken")
	@Expose
	private String nextSyncToken;
	@SerializedName("items")
	@Expose
	private List<UserItem> items;
}
