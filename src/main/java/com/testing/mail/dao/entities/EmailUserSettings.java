package com.testing.mail.dao.entities;

import java.util.List;

import javax.persistence.Id;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.testing.mail.repo.entities.PROCESS;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@TypeAlias(value ="EmailSettings")
@Document
@NoArgsConstructor
public class EmailUserSettings {
	@Id
	private String id;
	private String email;
	private String userId;
	private String workSpaceId;
	private String displayName;
	private String replyToAddress;
	private String signature;
	private boolean primary;
	private boolean isDefault;
	private String sourceId;
	private String destId;
	private String fromCloudId;
	private String toCloudId;
	private boolean sendAs;
	private boolean delegates;
	private String verificationStatus;
	private List<ForwardingAddresses>forwardingAddresses;
	private UserAutoForwarding autoForwardSettings;
	private UserImap imapSettings;
	private UserPopSetting popSettings;
	private UserVocation vocationSetting;
	private PROCESS processStatus;
	private String errorDescription;
	private List<String> roles;
	private boolean updated;
	private boolean alias;
	private String threadBy;
	
}
