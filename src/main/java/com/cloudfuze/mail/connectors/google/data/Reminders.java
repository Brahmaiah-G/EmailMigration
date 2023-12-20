package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Reminders {
	@SerializedName("useDefault")
	@Expose
	private Boolean useDefault;
	@SerializedName("overrides")
	@Expose
	private List<Overrides> overrides;
}
