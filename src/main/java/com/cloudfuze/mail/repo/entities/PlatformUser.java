package com.cloudfuze.mail.repo.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "Users")
@TypeAlias(value = "Users")
@Setter@Getter @ToString
public class PlatformUser implements Serializable{
	

	@Transient
	private static final long serialVersionUID = 7202229780479881858L;

	@Id
	private String id;
	
	@Field(name = "name", targetType = FieldType.STRING)
	private String name;
	
	@Field(name="email", targetType = FieldType.STRING)
	@Indexed(unique=true, background = true, name = "UniqueEmailIndex")
	private String email;
	@Field(name = "password" , targetType = FieldType.STRING)
	private String password;
	@Field(name = "phoneNumber", targetType = FieldType.STRING)
	private String phoneNumber;
	@Field(name="createdAt" , targetType = FieldType.TIMESTAMP)
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private Date createDateTime;
	
	@Field(name = "publicId", targetType = FieldType.STRING)
	private UUID publicId;
	@Field(name = "ent", targetType = FieldType.STRING)
	private String ent;
	@Field(name = "test", targetType = FieldType.BOOLEAN)
	private boolean test = false;
	
}
