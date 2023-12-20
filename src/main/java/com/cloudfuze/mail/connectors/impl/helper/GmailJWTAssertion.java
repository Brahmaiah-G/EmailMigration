package com.cloudfuze.mail.connectors.impl.helper;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.Signature;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GmailJWTAssertion{

	public GmailJWTAssertion() {
	}
	@Value("${com.connect.cloud.key.env}")
	String env;

	public String buildJWTClaimSetForGmail(String userEmail, String clientEmail) throws JSONException {
		String AUD = "https://www.googleapis.com/oauth2/v4/token";
		String ISS = clientEmail;
		String AUTH_PERMISSIONS ="https://www.googleapis.com/auth/drive " + 
				"https://www.googleapis.com/auth/userinfo.profile " + 
				"https://www.googleapis.com/auth/userinfo.email " + 
				"https://www.googleapis.com/auth/admin.directory.user.readonly " + 
				"https://www.googleapis.com/auth/admin.directory.user " + 
				"https://www.googleapis.com/auth/admin.directory.group.readonly " + 
				"https://www.googleapis.com/auth/admin.directory.group " + 
				"https://www.googleapis.com/auth/contacts " + 
				"https://www.googleapis.com/auth/gmail.compose " + 
				"https://mail.google.com/ " + 
//				"https://www.googleapis.com/auth/drive.file "+
//				"https://www.googleapis.com/auth/drive.metadata "+
				"https://www.googleapis.com/auth/gmail.labels " + 
				"https://www.googleapis.com/auth/gmail.settings.basic " + 
				"https://www.googleapis.com/auth/gmail.settings.sharing " + 
				"https://www.googleapis.com/auth/gmail.insert " + 
				"https://www.googleapis.com/auth/calendar " + 
				"https://www.googleapis.com/auth/calendar.events " + 
				"https://www.googleapis.com/auth/calendar.settings.readonly " + 
				//"https://www.googleapis.com/auth/admin.directory.domain "+
				"https://www.googleapis.com/auth/calendar.readonly ";
		//String AUTH_PERMISSIONS = "https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/admin.directory.user.readonly https://www.googleapis.com/auth/admin.directory.user https://www.googleapis.com/auth/admin.directory.group";

		JSONObject claimSet = new JSONObject();
		claimSet.put("iss", ISS);
		claimSet.put("scope", AUTH_PERMISSIONS);
		claimSet.put("sub", userEmail);
		claimSet.put("aud", AUD);
		long iat = System.currentTimeMillis()/1000;
		claimSet.put("exp", iat + 3600);
		claimSet.put("iat", iat);
		return claimSet.toString();
	}


	public String createJWT(String userEmailId,String clientEmail) throws Exception {
		StringBuilder jwtBuilder = new StringBuilder();

		jwtBuilder.append("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
		jwtBuilder.append(".");
		//Step 2: JWTClaimSet
		String jwtClaimSet = null;
		jwtClaimSet = buildJWTClaimSetForGmail(userEmailId, clientEmail);

		String base64EncodedJWTClaimSet = toBase64EncodedString(jwtClaimSet);
		jwtBuilder.append(base64EncodedJWTClaimSet);

		//Step 3: JWTSignature
		String content = jwtBuilder.toString();
		String jwtSignature = null;
		byte[] signedContentAsByteArray = buildJWTSignature(content);
		jwtSignature = toBase64EncodedString(signedContentAsByteArray);
		jwtBuilder.append(".").append(jwtSignature);

		//Return the JWT string
		return jwtBuilder.toString();
	}

	public String toBase64EncodedString (String stringToEncode) throws UnsupportedEncodingException {
		Base64 encoderDecoder = new Base64();	
		return new String(encoderDecoder.encode(toUTF8AsBytes(stringToEncode)), "UTF-8"); 
	}
	public String toBase64EncodedString (byte[] bytesArrayToEncode) throws UnsupportedEncodingException {
		Base64 encoderDecoder = new Base64();	
		return new String(encoderDecoder.encode(bytesArrayToEncode), "UTF-8");
	}

	public byte[] buildJWTSignature(String content) throws Exception {

		PrivateKey privateKey = buildPrivateKey();
		Signature rsaSha256Signature = Signature.getInstance("SHA256withRSA");
		rsaSha256Signature.initSign(privateKey);
		rsaSha256Signature.update(toUTF8AsBytes(content));
		return rsaSha256Signature.sign();

	}

	public byte [] toUTF8AsBytes (String stringToEncode) throws UnsupportedEncodingException {
		return stringToEncode.getBytes("UTF-8");
	}	




	public static PrivateKey buildPrivateKey() throws Exception {
		String env = System.getProperty("com.connect.cloud.key.env");
		Resource resource = null;
		if(StringUtils.isBlank(env)) {
			env = "emailMigration";
		}

		log.warn("env variable : "+env);
		if(env.equalsIgnoreCase("prod") || env.equalsIgnoreCase("cloudfuze")){
			resource =
					GmailJWTAssertion.getResource("gsuiteprod/Production-2c021d5c11b2.p12");

		}else if(env.equalsIgnoreCase("emailMigration")) {
			resource =
					GmailJWTAssertion.getResource("emailmigration-377717-0e8f642fb4d3.p12");
		}
		
		String pwd= "notasecret";
		String alias = "privatekey";
		char[] filePassword = pwd.toCharArray();
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(resource.getInputStream(), filePassword);
		PrivateKeyEntry keyEntry = (PrivateKeyEntry) keyStore.getEntry(alias, new KeyStore.PasswordProtection(filePassword));
		PrivateKey privateKey = keyEntry.getPrivateKey();
		return privateKey;
	}

	public static Resource getResource(String location){
		return new ClassPathResource(location);
	}

}


