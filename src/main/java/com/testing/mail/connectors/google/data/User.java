package com.testing.mail.connectors.google.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.springframework.data.mongodb.core.mapping.Language;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("primaryEmail")
	@Expose
	private String primaryEmail;
	@SerializedName("name")
	@Expose
	private Name name;
	@SerializedName("isAdmin")
	@Expose
	private Boolean isAdmin;
	@SerializedName("isDelegatedAdmin")
	@Expose
	private Boolean isDelegatedAdmin;
	@SerializedName("lastLoginTime")
	@Expose
	private String lastLoginTime;
	@SerializedName("creationTime")
	@Expose
	private String creationTime;
	@SerializedName("agreedToTerms")
	@Expose
	private Boolean agreedToTerms;
	@SerializedName("suspended")
	@Expose
	private Boolean suspended;
	@SerializedName("archived")
	@Expose
	private Boolean archived;
	@SerializedName("changePasswordAtNextLogin")
	@Expose
	private Boolean changePasswordAtNextLogin;
	@SerializedName("ipWhitelisted")
	@Expose
	private Boolean ipWhitelisted;
	@SerializedName("emails")
	@Expose
	private List<Email> emails = null;
	@SerializedName("phones")
	@Expose
	private List<Phone> phones = null;
//	@SerializedName("languages")
//	@Expose
//	private List<Language> languages = null;
	@SerializedName("customerId")
	@Expose
	private String customerId;
	@SerializedName("orgUnitPath")
	@Expose
	private String orgUnitPath;
	@SerializedName("isMailboxSetup")
	@Expose
	private Boolean isMailboxSetup;
	@SerializedName("isEnrolledIn2Sv")
	@Expose
	private Boolean isEnrolledIn2Sv;
	@SerializedName("isEnforcedIn2Sv")
	@Expose
	private Boolean isEnforcedIn2Sv;
	@SerializedName("includeInGlobalAddressList")
	@Expose
	private Boolean includeInGlobalAddressList;
	@SerializedName("suspensionReason")
	@Expose
	private String suspensionReason;
	@SerializedName("additionalProperties")
	@Expose
	private Map<String, Object> additionalProperties = new HashMap<>();
	@SerializedName("nonEditableAliases")
	@Expose
	private List<String>nonEditableAliases = null;
	@SerializedName("aliases")
	@Expose
	@JsonIgnore
	private List<String>aliases;
	@JsonIgnore
	private List<ExternalIds>externalIds;
	@JsonIgnore
	private List<Organizations> organizations;
	private String recoveryEmail;
	@JsonIgnore
	private Gender gender;
}