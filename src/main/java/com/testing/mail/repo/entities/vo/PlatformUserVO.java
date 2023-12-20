package com.testing.mail.repo.entities.vo;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class PlatformUserVO {
	
	//User public UUID
	String id;
	
	//User Name
	String name;
	
	// Users registered Email Address
	String email;
	
	//users login site
	String ent;
}
