
package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ContactsList {

	@SerializedName("value")
	@Expose
	private List<ContactValue> value;
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("@odata.nextLink")
	@Expose
	private String odataNextLink;
}
