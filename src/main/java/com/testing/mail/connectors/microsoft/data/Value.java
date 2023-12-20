package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Value {

	@SerializedName("@odata.etag")
	@Expose
	private String odataEtag;
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
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
	private List<String> categories = null;
	@SerializedName("receivedDateTime")
	@Expose
	private String receivedDateTime;
	@SerializedName("sentDateTime")
	@Expose
	private String sentDateTime;
	@SerializedName("hasAttachments")
	@Expose
	private Boolean hasAttachments;
	@SerializedName("internetMessageId")
	@Expose
	private String internetMessageId;
	@SerializedName("subject")
	@Expose
	private String subject;
	@SerializedName("bodyPreview")
	@Expose
	private String bodyPreview;
	@SerializedName("importance")
	@Expose
	private String importance;
	@SerializedName("parentFolderId")
	@Expose
	private String parentFolderId;
	@SerializedName("conversationId")
	@Expose
	private String conversationId;
	@SerializedName("conversationIndex")
	@Expose
	private String conversationIndex;
	@SerializedName("isDeliveryReceiptRequested")
	@Expose
	private Object isDeliveryReceiptRequested;
	@SerializedName("isReadReceiptRequested")
	@Expose
	private Boolean isReadReceiptRequested;
	@SerializedName("isRead")
	@Expose
	private Boolean isRead;
	@SerializedName("isDraft")
	@Expose
	private Boolean isDraft;
	@SerializedName("webLink")
	@Expose
	private String webLink;
	@SerializedName("inferenceClassification")
	@Expose
	private String inferenceClassification;
	@SerializedName("body")
	@Expose
	private Body body;
	@SerializedName("sender")
	@Expose
	private Emailer sender;
	@SerializedName("from")
	@Expose
	private Emailer from;
	@SerializedName("toRecipients")
	@Expose
	private List<Emailer> toRecipients = null;
	@SerializedName("ccRecipients")
	@Expose
	private List<Emailer> ccRecipients = null;
	@SerializedName("bccRecipients")
	@Expose
	private List<Emailer> bccRecipients = null;
	@SerializedName("replyTo")
	@Expose
	private List<Emailer> replyTo = null;
	@SerializedName("flag")
	@Expose
	private Flag flag;
	@JsonIgnore
	@SerializedName("@removed")
	@Expose
	private Removed removed;
	@JsonIgnore
	@SerializedName("event")
	@Expose
	private CalenderViewValue event;
	@JsonIgnore
	@SerializedName("error")
	@Expose
	private Error error;

}
