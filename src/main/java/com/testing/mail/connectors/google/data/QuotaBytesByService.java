package com.testing.mail.connectors.google.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class QuotaBytesByService {
	@SerializedName("serviceName")
	@Expose
	private String serviceName;
	@SerializedName("bytesUsed")
	@Expose
	private String bytesUsed;
}
