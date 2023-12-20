package com.cloudfuze.mail.repo.entities;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

//import com.cloudfuze.connect.enums.SaaSVendorLabel;

import lombok.Getter;
import lombok.Setter;

@Setter@Getter
@Document(collection = "OAuthKeys")
public class OAuthKey implements Serializable{
	
	@Transient
	private static final long serialVersionUID = 5859853458953462170L;

	@Id
	String id;
	
	//@Field(name = "vendor", targetType = FieldType.STRING)
	private String cloudName;
	
	@Field(name = "clientId", targetType = FieldType.STRING)
	private String clientId;
	
	@Field(name = "clientSecret", targetType = FieldType.STRING)
	private String clientSecret;	

	@Field(name = "redirectUrl", targetType = FieldType.STRING)
	private String redirectUrl;
	
	@Field(name = "appRedirectUrl", targetType = FieldType.STRING)
	private String appRedirectUrl;
	private String cilentEmail;
}
