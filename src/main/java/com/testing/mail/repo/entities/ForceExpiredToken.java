package com.testing.mail.repo.entities;

import java.io.Serializable;
import java.util.Date;

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



/**
 *	Models a JWT token to be validated for force expiry with fields storing its original expiry
 *
 */
@Document(collection = "ForceExpiredTokens")
@TypeAlias(value = "ForceExpiredToken")
@Setter@Getter@NoArgsConstructor
public class ForceExpiredToken implements Serializable{
	

	@Transient
	private static final long serialVersionUID = 7787793868567226433L;
	/* 
	 * Auto-Generated Unique Id 
	 */
	@Id
	private String Id;
	
	/**
	 *  jwt token forced that was force invalidated before expiry
	 */
	@Field(name = "name", targetType = FieldType.STRING)
	private String token;
	
	/**
	 * @Param stating original expiry dtae-time of the token
	 *
	 */
	@Field(name="expiresAt" , targetType = FieldType.TIMESTAMP)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private Date originalExpiry;
	
}