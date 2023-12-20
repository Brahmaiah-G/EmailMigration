package com.testing.mail.connectors.microsoft.data;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class Response {
	@Expose
	@SerializedName("id")
	private String id;
	@Expose
	@SerializedName("status")
	private Integer status;
	@Expose
	@SerializedName("headers")
	private Headers headers;
	@Expose
	@SerializedName("body")
	private Value value;

}
