package com.testing.mail.config;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.testing.mail.repo.entities.PlatformUser;
import com.testing.mail.repo.impl.MongoOpsManager;
import com.github.f4b6a3.uuid.UuidCreator;

@Service
public class PlatformUserDetailSerice implements UserDetailsService {
	
	@Autowired 
	private MongoOpsManager mongoManager;
	
	private ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

	@Override
	public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
		
		//Construct uuid from string
		UUID userPublicId = UuidCreator.fromString(id);
		//fetch user byr public uuid
		PlatformUser user = mongoManager.findUserByPublicId(userPublicId);
		//return user details
		return  new  User(user.getId(), user.getPassword(), authorities);
	}
}
