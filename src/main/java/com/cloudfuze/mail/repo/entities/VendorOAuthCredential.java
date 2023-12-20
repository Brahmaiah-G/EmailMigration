package com.cloudfuze.mail.repo.entities;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Setter@Getter
@Document(collection = "VendorOAuthCredentials")
@TypeAlias(value = "VendorOAuthCredential")
public class VendorOAuthCredential implements Serializable{

	@Transient
	private static final long serialVersionUID = 941271242529963863L;

	@Id
	private String id;
		
	private CLOUD_NAME cloudName;
	
	@Field(name = "domainName", targetType = FieldType.STRING)
	private String domainName;
	
	@Field(name = "adminEmail", targetType = FieldType.STRING)
	private String adminEmail;
	
	@Field(name = "accessToken", targetType = FieldType.STRING)
	private String accessToken;
	
	@Field(name = "refreshToken", targetType = FieldType.STRING)
	private String refreshToken;
	
	@JsonIgnore
	@Field(name="expiresAt" , targetType = FieldType.TIMESTAMP)
	private LocalDateTime expiresAt ;
	
	@Field(name="lastRefreshed" , targetType = FieldType.TIMESTAMP)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private LocalDateTime lastRefreshed;
	
	@Field(name = "userId", targetType = FieldType.STRING)
	private String userId;
	
	@Field(name = "vendorId", targetType = FieldType.STRING)
	private String vendorId;
}
