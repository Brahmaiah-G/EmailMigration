package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalendarPermissions {

	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("isRemovable")
	@Expose
	private Boolean isRemovable;
	@SerializedName("isInsideOrganization")
	@Expose
	private Boolean isInsideOrganization;
	@SerializedName("role")
	@Expose
	private String role;
	@SerializedName("allowedRoles")
	@Expose
	private List<String> allowedRoles;
	@SerializedName("emailAddress")
	@Expose
	private EmailAddress emailAddress;
}
