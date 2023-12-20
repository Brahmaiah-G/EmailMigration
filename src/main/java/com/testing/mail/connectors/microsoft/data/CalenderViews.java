package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalenderViews {
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("value")
	@Expose
	private List<CalenderViewValue> value;
	@SerializedName("@odata.nextLink")
	@Expose
	private String odataNextLink;
}
