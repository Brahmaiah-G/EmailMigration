package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class Group {

	@SerializedName("kind")
	@Expose
	private String kind;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("etag")
	@Expose
	private String etag;
	@SerializedName("email")
	@Expose
	private String email;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("directMembersCount")
	@Expose
	private long directMembersCount;
	@SerializedName("description")
	@Expose
	private String description;
	@SerializedName("adminCreated")
	@Expose
	private Boolean adminCreated;
	
	@SerializedName("aliases")
	@Expose
	private List<String> aliases = null;

	@SerializedName("nonEditableAliases")
	@Expose
	private List<String> nonEditableAliases = null;
}
