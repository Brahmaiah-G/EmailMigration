package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

public class Domain {

	@SerializedName("authenticationType")
	@Expose
	private String authenticationType;
	@SerializedName("availabilityStatus")
	@Expose
	private Object availabilityStatus;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("isAdminManaged")
	@Expose
	private Boolean isAdminManaged;
	@SerializedName("isDefault")
	@Expose
	private Boolean isDefault;
	@SerializedName("isInitial")
	@Expose
	private Boolean isInitial;
	@SerializedName("isRoot")
	@Expose
	private Boolean isRoot;
	@SerializedName("isVerified")
	@Expose
	private Boolean isVerified;
	@SerializedName("supportedServices")
	@Expose
	private List<String> supportedServices = null;
	@SerializedName("passwordValidityPeriodInDays")
	@Expose
	private long passwordValidityPeriodInDays;
	@SerializedName("passwordNotificationWindowInDays")
	@Expose
	private long passwordNotificationWindowInDays;
	@SerializedName("state")
	@Expose
	private Object state;

	

}