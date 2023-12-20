package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class DriveAbout {
	@SerializedName("rootFolderId")
	@Expose
	private String rootFolderId;
	@SerializedName("quotaBytesByService")
	@Expose
	private List<QuotaBytesByService> quotaBytesByService;
}
