package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AttachMents {
	@SerializedName("size")
	@Expose
	private Integer size;
	@SerializedName("data")
	@Expose
	private String data;
}
