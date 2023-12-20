package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Header {
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("value")
	@Expose
	private String value;
}
