package com.testing.mail.service;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.testing.mail.exceptions.InvaildCredentialsException;
import com.testing.mail.exceptions.TokenExpiredException;
import com.testing.mail.exceptions.UserNotFoundException;
import com.testing.mail.model.request.NewPasswordVO;
import com.testing.mail.model.request.RegisterUserVO;
import com.testing.mail.repo.entities.PasswordResetToken;
import com.testing.mail.repo.entities.PlatformUser;
import com.testing.mail.repo.entities.vo.PlatformUserVO;
import com.testing.mail.repo.impl.MongoOpsManager;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter@Getter@Slf4j
@Service
public class PlatformUserService {

	@Autowired 
	private MongoOpsManager mongoManager;

	@Autowired
	private JWTAuthService jwtService;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired 
	private EmailService mailService;

	@Autowired
	private PlatformUserPasswordModificationService pwdService;


	public Boolean userExists(String email) {

		Boolean exists = false;

		//fetching user with given email
		PlatformUser user = mongoManager.findUserByEmail(email);

		//validating if user exists
		if(user != null)
			exists = true;

		log.info("User email {} ",(user==null?"Doesn't Exist":"Already Exists"));		
		return exists;		
	}

	public Boolean userExists(String email,String ent) {

		Boolean exists = false;

		//fetching user with given email
		PlatformUser user = mongoManager.findUserByEmailAndEnt(email,ent);

		//validating if user exists
		if(user != null)
			exists = true;

		log.info("User email {} ",(user==null?"Doesn't Exist":"Already Exists"));		
		return exists;		
	}

	public PlatformUser registerUser(RegisterUserVO vo) {

		PlatformUser user = new PlatformUser();
		/* 
		 * copy from request to entity 
		 */
		//Name Case Correction and set to entity
		user.setName( WordUtils.capitalizeFully(vo.getName()));
		//Encrypt Password and set to entity
		user.setPassword(encoder.encode(vo.getPassword()));
		//Email
		user.setEmail(vo.getEmail());
		//Contact
		user.setPhoneNumber(vo.getPhoneNumber());
		//Generate a UUID as user public id 		
		user.setPublicId(UuidCreator.getRandomBased());
		//Set Creation time
		user.setCreateDateTime(new Date(System.currentTimeMillis()));
		// setting Ent for multiple instances users creation with the same email but differnt password so 503 won't occur and won't collide
		user.setEnt(vo.getEnt());

		log.info("User Details {}", user.toString());
		//Persist User in db
		user = mongoManager.registerUser(user);
		mailService.registrationSuccessfulMail(user.getEmail(), user.getName(),user.getEnt());

		return user;
	}

	public PlatformUser findUserByEmail(String email) {
		return mongoManager.findUserByEmail(email);
	}

	public PlatformUserVO verifyUser(String email,String password,String ent) {

		//fetch USer from db
		PlatformUser user = mongoManager.findUserByEmailAndEnt(email,ent);

		if(user==null)
			throw new UserNotFoundException();
		Boolean verifyPassword = encoder.matches(password, user.getPassword());
		if(!verifyPassword)
			throw new InvaildCredentialsException("password misMatches");			

		PlatformUserVO uservo = new PlatformUserVO();		
		uservo.setId(user.getPublicId().toString());
		uservo.setName(user.getName());
		uservo.setEmail(user.getEmail());
		uservo.setEnt(user.getEnt());
		return uservo;		
	}

	public Boolean doPasswordReset(String token, String password) {

		boolean isTokenValid = pwdService.verifyToken(token);

		if(!isTokenValid)
			throw new TokenExpiredException("Token Expired Try Again");

		String tokenId = pwdService.uncompressToken(token);
		UUID tokenUUID = UuidCreator.fromString(tokenId);

		PlatformUser user = mongoManager.findPlatformUserByPasswordResetToken(tokenUUID);

		//Get new Password Encrypt and set to entity
		String newPassword = encoder.encode(password);
		user.setPassword(newPassword);
		//persist in db
		user = mongoManager.updatePlatformUser(user);

		if(user==null)
			return false;
		return true;
	}
	public Boolean setNewPassword(NewPasswordVO vo, String userId) {

		//Fetch User
		PlatformUser user = mongoManager.findUserById(userId);
		//Verify current password matches given password
		Boolean isPasswordValid = encoder.matches(vo.getOrginalPassword(), user.getPassword());
		// if password doesnt match throw exception
		if(!isPasswordValid)
			throw new InvaildCredentialsException("Incorrect Password");
		// check if new password is same as old password
		if(vo.getOrginalPassword().equals(vo.getNewPassword()))
			throw new InvaildCredentialsException("New Password cannot be same as Old Password");
		//else encyprt and set new password
		user.setPassword(encoder.encode(vo.getNewPassword()));
		// persist in db
		user = mongoManager.updatePlatformUser(user);
		if(user==null)
			return false;
		return true;
	}

	public Boolean verifyPasswordResetToken(String token, String email) {

		Boolean isTokenValid = pwdService.verifyToken(token);
		String userMail = email.toLowerCase();

		if(!isTokenValid)
			return false;

		String tokenId = pwdService.uncompressToken(token);
		UUID tokenUUID = UuidCreator.fromString(tokenId);	
		PlatformUser user = mongoManager.findPlatformUserByPasswordResetToken(tokenUUID);		
		Boolean hasSameEmail = userMail.equals(user.getEmail());

		if(!hasSameEmail)
			return false;

		return true;
	}

	public Boolean generatePasswordResetEmail(String email){

		//fetching given user
		PlatformUser user = mongoManager.findUserByEmail(email);

		//generate random uuid for reset token
		UUID tokenId = UuidCreator.getRandomBased();

		//Generate Reset Token
		String resetToken = pwdService.generateToken(tokenId.toString(),user.getName());

		//Persist token in db
		PasswordResetToken passwordResetToken = pwdService.getPasswordResetToken(tokenId,resetToken,user.getPublicId());
		passwordResetToken = mongoManager.savePasswordResetToken(passwordResetToken);

		//Generate Reset Verification Mail
		return mailService.passwordResetMail(email, user.getName(), resetToken);

		//return success;		
	}



}