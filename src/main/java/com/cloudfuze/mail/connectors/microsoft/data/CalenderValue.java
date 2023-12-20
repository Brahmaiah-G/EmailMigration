package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalenderValue {
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("color")
	@Expose
	private String color;
	@SerializedName("hexColor")
	@Expose
	private String hexColor;
	@SerializedName("isDefaultCalendar")
	@Expose
	private Boolean isDefaultCalendar;
	@SerializedName("changeKey")
	@Expose
	private String changeKey;
	@SerializedName("canShare")
	@Expose
	private Boolean canShare;
	@SerializedName("canViewPrivateItems")
	@Expose
	private Boolean canViewPrivateItems;
	@SerializedName("isShared")
	@Expose
	private Boolean isShared;
	@SerializedName("isSharedWithMe")
	@Expose
	private Boolean isSharedWithMe;
	@SerializedName("canEdit")
	@Expose
	private Boolean canEdit;
	@SerializedName("calendarGroupId")
	@Expose
	private String calendarGroupId;
	@SerializedName("allowedOnlineMeetingProviders")
	@Expose
	private List<String> allowedOnlineMeetingProviders;
	@SerializedName("defaultOnlineMeetingProvider")
	@Expose
	private String defaultOnlineMeetingProvider;
	@SerializedName("isTallyingResponses")
	@Expose
	private Boolean isTallyingResponses;
	@SerializedName("isRemovable")
	@Expose
	private Boolean isRemovable;
	@SerializedName("owner")
	@Expose
	private Owner owner;
}
