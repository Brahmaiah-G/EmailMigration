package com.cloudfuze.mail.connectors.microsoft.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Language {
	@SerializedName("locale")
	@Expose
	private String locale;
	@SerializedName("displayName")
	@Expose
	private String displayName;
}
