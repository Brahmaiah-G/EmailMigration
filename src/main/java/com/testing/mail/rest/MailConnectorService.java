package com.testing.mail.rest;

/**
 * @author BrahmaiahG
*/
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.GMailConnector;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.dao.entities.Enums.Status;
import com.testing.mail.model.request.AuthorizationCodeDelegate;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(path = "/cloud")
public class MailConnectorService {

	@Autowired
	DBConnectorService connectorService;
	
	@Autowired
	GMailConnector gMailConnector;
	@Autowired
	OutLookMailConnector outLookMailConnector;
	@Autowired
	MailServiceFactory mailServiceFactory;
	@Autowired
    MailMappingConnector mailMappingConnector;
	
	@GetMapping(path = "/oauthkeys/{cloud}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getOAuthKeys(@PathVariable(name = "cloud", required = true) CLOUD_NAME vendor) {
		log.debug("cloud Name: " + vendor);
		OAuthKey oAuthkey = null;
		try {
			oAuthkey = mailMappingConnector.getOAuthKeys(vendor);			
		} catch (IllegalArgumentException ex) {
			ResponseEntity.badRequest().body("Entered Incorrect cloud Name");
		}
		return ResponseEntity.ok(oAuthkey);
	}

	@PostMapping(path = "/add/{cloud}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> addCloudMapping(@PathVariable(name = "cloud") CLOUD_NAME vendor,
				@RequestBody AuthorizationCodeDelegate delegator,@RequestAttribute("userId")String userId) {

		boolean isValid = false;
		isValid = mailMappingConnector.getAccessToken(vendor, userId, delegator.getAuthCode());
		if(isValid)
			return ResponseEntity.ok("Cloud added SuccessFully");
		
		return ResponseEntity.badRequest().body("Unable add cloud. Please try again after sometime");
	}
	
	@PostMapping(path = "/add-cloud/{cloud}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createVendor(
			@PathVariable(name = "cloud", required = true) CLOUD_NAME cloudName, 
			@RequestBody VendorOAuthCredential credentials, @RequestAttribute("userId") String userId) {

		Status isValid = mailMappingConnector.saveVendorFromXchange(cloudName, userId, credentials);
		if(isValid == Status.SUCCESS)
			return ResponseEntity.ok("Cloud Mapped SuccessFully. Synchronization in Progress");
		
		return ResponseEntity.badRequest().body("Unable to Map Cloud. Please try again after sometime"+isValid.toString());
	}
	
	
	@GetMapping(path = "/reAuth/{cloud}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> reAuthenticateUsers(
			@PathVariable(name = "cloud", required = true) String cloudId, 
			@RequestAttribute("userId") String userId,@RequestParam("cloudName")String cloudName) {

		boolean refreshed = mailMappingConnector.refreshUsers(cloudId,cloudName);
		if(refreshed)
			return ResponseEntity.ok("Cloud Mapped SuccessFully. Synchronization in Progress");
		
		return ResponseEntity.badRequest().body("Unable to Map Cloud. Please try again after sometime:"+refreshed);
	}
	
	
	
	@DeleteMapping(path = "/remove/{cloud}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> removeCloudAccess(
			@PathVariable(name = "cloud", required = true) CLOUD_NAME cloudName, 
			@RequestBody String adminEmail, @RequestAttribute("userId") String userId) {

		boolean isValid = mailMappingConnector.removeCloudAccess(adminEmail, userId, cloudName);
		if(isValid)
			return ResponseEntity.ok("Cloud Deleted SuccessFully");
		
		return ResponseEntity.badRequest().body("Unable to Delete Cloud. Please try again after sometime:"+isValid);
	}
	
	@GetMapping("/admin/{adminMemberId}")
	public ResponseEntity<?> getCloudsByAdminMemberId(@PathVariable("adminMemberId")String adminMemberId,@RequestAttribute("userId")String userId,@RequestParam("cloudName")String cloudName){
		log.warn("==Getting the members for the adminClud=="+adminMemberId);
		return HttpUtils.Ok(connectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, adminMemberId,cloudName));
	}
	
}
