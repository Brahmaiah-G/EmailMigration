package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class UserInfo {

	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("email")
	@Expose
	private String email;
	@SerializedName("verified_email")
	@Expose
	private Boolean verifiedEmail;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("given_name")
	@Expose
	private String givenName;
	@SerializedName("family_name")
	@Expose
	private String familyName;
	@SerializedName("picture")
	@Expose
	private String picture;
	@SerializedName("locale")
	@Expose
	private String locale;
	@SerializedName("hd")
	@Expose
	private String hd;
}