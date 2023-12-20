package com.cloudfuze.mail.repo.entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.Data;

@Data
@Document
@TypeAlias("memberDetails")
public class MemberDetails {
	@Id
	private String id;
	private String userId;
	private String adminCloudId;
	private List<String>members;
	private CLOUD_NAME cloudName;
}
