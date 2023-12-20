package com.cloudfuze.mail.repo.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "PasswordResetTokens")
@TypeAlias(value = "PasswordResetToken")
@Setter@Getter@NoArgsConstructor@ToString
public class PasswordResetToken implements Serializable{
	
	@Transient
	private static final long serialVersionUID = 2294197238618337231L;
	
	@Id
	String Id;
		
	@Field(name = "userId", targetType = FieldType.STRING)
	private UUID userPublicId;
	
	@Field(name="expiresAt" , targetType = FieldType.TIMESTAMP)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private Date originalExpiry;
	
	@Field(name = "tokenId", targetType = FieldType.STRING)
	private UUID tokenId;
	
	@Field(name = "token", targetType = FieldType.STRING)
	private String token;

}
