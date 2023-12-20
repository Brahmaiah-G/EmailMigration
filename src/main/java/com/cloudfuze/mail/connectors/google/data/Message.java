package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Message {

	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("threadId")
	@Expose
	private String threadId;
	@SerializedName("labelIds")
	@Expose
	private List<String> labelIds = null;

	
	public void setLabelIds(List<String> labelIds) {
		this.labelIds = labelIds;
	}

}
