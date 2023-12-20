package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class History {
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("messages")
	@Expose
	private List<Message> messages;
	@SerializedName("messagesAdded")
	@Expose
	private List<MessagesAdded> messagesAdded;
	@SerializedName("messagesDeleted")
	@Expose
	private List<MessagesAdded>messagesDeleted;
	@SerializedName("labelsRemoved")
	@Expose
	private List<MessagesAdded>labelsRemoved;
	@SerializedName("labelsAdded")
	@Expose
	private List<MessagesAdded>labelsAdded;
}
