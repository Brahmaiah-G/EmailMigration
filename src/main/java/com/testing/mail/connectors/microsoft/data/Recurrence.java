package com.testing.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Recurrence {

	@SerializedName("pattern")
	@Expose
	private Pattern pattern;
	@SerializedName("range")
	@Expose
	private Range range;
}
