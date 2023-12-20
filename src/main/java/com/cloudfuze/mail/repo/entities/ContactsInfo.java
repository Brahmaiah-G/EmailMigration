package com.cloudfuze.mail.repo.entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cloudfuze.mail.repo.entities.PROCESS;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document
@TypeAlias(value="ContactsInfo")
public class ContactsInfo {

	@Id
	private String id;
	private String sourceId;
	private String destId;
	private String emailWorkSpaceId;
	private String userId;
	private PROCESS processStatus;
	private List<String>emailAddress;
	private String name;
	private List<String>phoneNumbers;
	private String converPhotoId;
	private String type;
	private String threadBy;
	private String jobId;
	private String errorDescription;
}
