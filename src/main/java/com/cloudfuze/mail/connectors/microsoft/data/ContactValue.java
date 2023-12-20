package com.cloudfuze.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ContactValue {

	@SerializedName("@odata.etag")
	@Expose
	private String odataEtag;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("createdDateTime")
	@Expose
	private String createdDateTime;
	@SerializedName("lastModifiedDateTime")
	@Expose
	private String lastModifiedDateTime;
	@SerializedName("changeKey")
	@Expose
	private String changeKey;
	@SerializedName("categories")
	@Expose
	private List<Object> categories;
	@SerializedName("parentFolderId")
	@Expose
	private String parentFolderId;
	@SerializedName("birthday")
	@Expose
	private Object birthday;
	@SerializedName("fileAs")
	@Expose
	private String fileAs;
	@SerializedName("displayName")
	@Expose
	private String displayName;
	@SerializedName("givenName")
	@Expose
	private String givenName;
	@SerializedName("initials")
	@Expose
	private Object initials;
	@SerializedName("middleName")
	@Expose
	private Object middleName;
	@SerializedName("nickName")
	@Expose
	private Object nickName;
	@SerializedName("surname")
	@Expose
	private String surname;
	@SerializedName("title")
	@Expose
	private Object title;
	@SerializedName("yomiGivenName")
	@Expose
	private Object yomiGivenName;
	@SerializedName("yomiSurname")
	@Expose
	private Object yomiSurname;
	@SerializedName("yomiCompanyName")
	@Expose
	private Object yomiCompanyName;
	@SerializedName("generation")
	@Expose
	private Object generation;
	@SerializedName("imAddresses")
	@Expose
	private List<Object> imAddresses;
	@SerializedName("jobTitle")
	@Expose
	private Object jobTitle;
	@SerializedName("companyName")
	@Expose
	private String companyName;
	@SerializedName("department")
	@Expose
	private Object department;
	@SerializedName("officeLocation")
	@Expose
	private Object officeLocation;
	@SerializedName("profession")
	@Expose
	private Object profession;
	@SerializedName("businessHomePage")
	@Expose
	private Object businessHomePage;
	@SerializedName("assistantName")
	@Expose
	private String assistantName;
	@SerializedName("manager")
	@Expose
	private String manager;
	@SerializedName("homePhones")
	@Expose
	private List<Object> homePhones;
	@SerializedName("mobilePhone")
	@Expose
	private String mobilePhone;
	@SerializedName("businessPhones")
	@Expose
	private List<Object> businessPhones;
	@SerializedName("spouseName")
	@Expose
	private String spouseName;
	@SerializedName("personalNotes")
	@Expose
	private String personalNotes;
	@SerializedName("children")
	@Expose
	private List<Object> children;
	@SerializedName("emailAddresses")
	@Expose
	private List<EmailAddress> emailAddresses;
	@SerializedName("homeAddress")
	@Expose
	private HomeAddress homeAddress;
	@SerializedName("businessAddress")
	@Expose
	private BusinessAddress businessAddress;
	@SerializedName("otherAddress")
	@Expose
	private OtherAddress otherAddress;
}
