package com.cloudfuze.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConferenceSolution {
	@SerializedName("key")
	@Expose
	private Key key;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("iconUri")
	@Expose
	private String iconUri;
}
