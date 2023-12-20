package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Payload {
	@SerializedName("mimeType")
	@Expose
	private String mimeType;
	@SerializedName("body")
	@Expose
	private Body body;
	@SerializedName("partId")
	@Expose
	private String partId;
	@SerializedName("filename")
	@Expose
	private String filename;
	@SerializedName("headers")
	@Expose
	private List<Header> headers;
	@SerializedName("parts")
	@Expose
	private List<Part> parts;
}
