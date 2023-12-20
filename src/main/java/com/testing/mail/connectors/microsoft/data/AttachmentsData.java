package com.testing.mail.connectors.microsoft.data;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
/**
 *POJO Related to Attachments 
*/
public class AttachmentsData {

	private String contentType;
	private String odataType;
	private String name;
	private long size;
	private String contentBytes;
	private boolean encoded;
	private String parentMessageId;
	private String parentFolderId;
	private boolean largeFile;
	private boolean completed;
	private String id;
	private boolean inline;

}
