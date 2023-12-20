package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class MemberList {
	
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("@odata.nextLink")
	@Expose
	private String odataNextLink;
	@SerializedName("value")
	@Expose
	private List<MemberValueVO> value;
	@SerializedName("@odata.deltaLink")
	@Expose
	private String odataDeltaLink;

	
}
