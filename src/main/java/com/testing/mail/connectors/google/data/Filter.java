package com.testing.mail.connectors.google.data;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Filter {
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("criteria")
	@Expose
	private Criteria criteria;
	@SerializedName("action")
	@Expose
	private Action action;
}
