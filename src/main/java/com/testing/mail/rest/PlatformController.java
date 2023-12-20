package com.testing.mail.rest;

import javax.validation.constraints.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.testing.mail.model.request.LoginRequestVo;
import com.testing.mail.model.request.NewPasswordVO;
import com.testing.mail.model.request.RegisterUserVO;
import com.testing.mail.repo.entities.PlatformUser;
import com.testing.mail.repo.entities.vo.PlatformUserVO;
import com.testing.mail.service.JWTAuthService;
import com.testing.mail.service.PlatformUserService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/app")
@Validated
@Slf4j@Setter@Getter
public class PlatformController {
	
	@Autowired 
	private PlatformUserService platformUserService;	
	
	@Autowired 
	private JWTAuthService jWTService; 

	/** 
	 * Register or Signup new user
	 */
	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestBody final RegisterUserVO user) {
		
		PlatformUser pUser = null;
		if (user != null) {
			/* 
			 * check if the user already exists
			 */			
			Boolean exists = platformUserService.userExists(user.getEmail(),user.getEnt());
			if(Boolean.TRUE.equals(exists)) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("User Email Already Exists, Try Logging In");
			}
			pUser = platformUserService.registerUser(user);
			if(pUser==null)
				log.warn("User Registration Failed - {}",user.getEmail());
			else
				log.info("User Registration Successful - {}",pUser.getEmail());	
		}
		return ResponseEntity.ok("User Registered Successfully, Please Login");		
	}
	
	/** 
	 * Check if user email already exists 
	 */
	@GetMapping("/users/check/{email}")
	public ResponseEntity<?> checkUserAlreadyExists(@PathVariable @Email String email){
		
		log.info("Verifying if email already exists {}",email);
		Boolean exists = platformUserService.userExists(email.toLowerCase());
		if(Boolean.TRUE.equals(exists)) 
			return ResponseEntity.badRequest().body("User Email Already Exists, Try Logging In");
		else
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Email Does not Exists");
	}
	
	/**
	 * Endpoint to Login to CloudFuzeConnect 
	 */
	@PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> login(@RequestBody LoginRequestVo vo, @RequestHeader(value = "Host") String host,@RequestHeader(value = "User-Agent") String userAgent) {
		
		log.info("User Login Process started with user -> " + vo.getEmail());
		
		//Verify User Credentials 
		PlatformUserVO uservo = platformUserService.verifyUser(vo.getEmail(), vo.getPassword(),vo.getEnt());
			
		// Generating JWT as response header
		HttpHeaders  responseHeaders = jWTService.generateSessionToken(uservo,host,userAgent);
	
		return ResponseEntity.ok().headers(responseHeaders).body(uservo);
	}
	
	/** 
	 * Request Password Reset Email
	 */
	@GetMapping(path = "/forgot-password/{email}")
	public ResponseEntity<?> requestForgotPassword(@PathVariable @Email String email){
		email = email.toLowerCase();
		ResponseEntity<?> response = null;
		Boolean exists = platformUserService.userExists(email);
		log.error(" User {} "+ (exists?"Exists":"Does not Exist"),email);
		if(Boolean.FALSE.equals(exists))
			response = ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Does Not Exists, Register First");
		else{
			
			Boolean isReset = platformUserService.generatePasswordResetEmail(email);
			if(Boolean.TRUE.equals(isReset))					
				response = ResponseEntity.status(HttpStatus.RESET_CONTENT).body("User Password Reset, Verify link sent to Email. Link Expires in 2 hrs");
			else
				throw new RuntimeException("Could not Process Request. Try Again After Sometime");
		}
		return response;
	}	
	
	/**
	 * EndPoint to validate Password Reset Token
	 * @param token
	 * @return Status of token validity
	 */	
	@PostMapping(path = "/pwd/verify/{token}/user/{email}")
	public ResponseEntity<?> verifyForgotPasswordToken(@PathVariable("token") String token,@PathVariable("email") @Email String email){
		
		Boolean isValid = platformUserService.verifyPasswordResetToken(token, email);
		if(Boolean.TRUE.equals(isValid))
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("Configure New Password");
		else 
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("InvalidToken");		
	}
	
	/** 
	 * Reset Forgotten password and set new password 
	 */ 
	@PostMapping(path = "/pwd/reset/{token}/{password}")
	public ResponseEntity<?> resetPassword(@PathVariable("token")String token , @PathVariable("password")String password){
		
		Boolean resetSuccess = platformUserService.doPasswordReset(token,password);
		if(Boolean.FALSE.equals(resetSuccess))
			return  ResponseEntity.status(HttpStatus.CREATED).body("Password Reset Success. Please Login");	

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Link Expired. Try Again");	
	}
			
	/**
	 * Change existing password
	 */	
	@PostMapping(path = "/pwd/new")
	public ResponseEntity<?> setNewPassword(@RequestBody NewPasswordVO vo, @RequestAttribute(name = "userId") String userId){
		
		Boolean resetSuccess =  false;
		//Validate Orginal Password and set new password 
		resetSuccess = platformUserService.setNewPassword(vo,userId);
		
		if(Boolean.TRUE.equals(resetSuccess))
			return  ResponseEntity.status(HttpStatus.ACCEPTED).body("Password Reset Success. Please Login");
		else
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected Error. Try Again in sometime");			
	}
	
	/** 
	 * On Logout Forcefully Expiring Tokens that still have validity
	 */ 
	@PutMapping("/logout")
	public ResponseEntity<?> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorization){

		Boolean expired = jWTService.forceExpireToken(authorization);
		if(Boolean.FALSE.equals(expired))
			return ResponseEntity.ok("Token Expired.User Logged out Successfully");
		return ResponseEntity.ok("Token Expired.User Logged out Successfully");
	}
	
	/**
	 * Resource endpoint to get PlatformUserBy Emailid.
	 * 
	 */
	@GetMapping("/user/{email}")
	public ResponseEntity<?> getPlatformUserByEmail(@PathVariable @Email String email) {
		log.debug("Get PlatformUserVO for email {} ", email);

		PlatformUser user = platformUserService.findUserByEmail(email);
		if (user != null) {
			PlatformUserVO vo = new PlatformUserVO();
			vo.setId(user.getPublicId().toString());
			vo.setEmail(user.getEmail());
			vo.setName(user.getName());
			return new ResponseEntity<>(vo, HttpStatus.OK);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
	}
}