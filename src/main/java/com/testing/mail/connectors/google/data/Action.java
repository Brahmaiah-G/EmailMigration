package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Action {
	@SerializedName("addLabelIds")
	@Expose
	private List<String> addLabelIds;
	@SerializedName("removeLabelIds")
	@Expose
	private List<String> removeLabelIds;
	@SerializedName("forward")
	@Expose
	private String forward;
}
