package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmailFoldersValue {
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("displayName")
	@Expose
	private String displayName;
	@SerializedName("parentFolderId")
	@Expose
	private String parentFolderId;
	@SerializedName("childFolderCount")
	@Expose
	private long childFolderCount;
	@SerializedName("unreadItemCount")
	@Expose
	private long unreadItemCount;
	@SerializedName("totalItemCount")
	@Expose
	private long totalItemCount;
	@SerializedName("sizeInBytes")
	@Expose
	private long sizeInBytes;
	@SerializedName("isHidden")
	@Expose
	private Boolean isHidden;
	@SerializedName("childFolders")
	@Expose
	private List<EmailFoldersValue> childFolders;


}
