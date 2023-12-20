package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WritebackConfiguration {
	@SerializedName("isEnabled")
	@Expose
	private Object isEnabled;
	@SerializedName("onPremisesGroupType")
	@Expose
	private Object onPremisesGroupType;
}
