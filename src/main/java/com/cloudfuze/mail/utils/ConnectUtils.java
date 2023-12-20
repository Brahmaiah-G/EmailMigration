package com.cloudfuze.mail.utils;

import static com.cloudfuze.mail.constants.Const.BEARER;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import com.cloudfuze.mail.connectors.impl.helper.GmailHelper;
import com.cloudfuze.mail.connectors.microsoft.data.RefreshTokenResult;
import com.cloudfuze.mail.constants.ExceptionConstants;
import com.cloudfuze.mail.exceptions.MailCreationException;
import com.cloudfuze.mail.exceptions.MailExceptionUtils;
import com.cloudfuze.mail.exceptions.MailMigrationException;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.OAuthKey;
import com.cloudfuze.mail.repo.entities.VendorOAuthCredential;
import com.cloudfuze.mail.service.DBConnectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class ConnectUtils {


	private static final ObjectMapper objMapper = new ObjectMapper();
	private static final Random randomGenerator = new Random();
	private static final String USER_AGENT_VALUE = "ISV|CloudFuze,Inc|CloudFuze/1.0";

	@Autowired
	private RestTemplate restTemplate;

	private static  RestTemplate restTemplate2;

	@Autowired 
	private DBConnectorService dbConnectorService;

	private static DBConnectorService dbConnectorService2;

	@Autowired
	private GmailHelper apiHelper;

	private static GmailHelper apiHelper2 ;

	private final static Client client = Client.create();

	@PostConstruct
	private void init() {
		apiHelper2 = this.apiHelper;
		restTemplate2 = this.restTemplate;
		dbConnectorService2 = this.dbConnectorService;
	}
	private static int SOCKET_TIMEOUT_IN_MS = 10 * 6000;
	private static SimpleClientHttpRequestFactory getClientHttpRequestFactory() 
	{
	    SimpleClientHttpRequestFactory clientHttpRequestFactory
	                      = new SimpleClientHttpRequestFactory();
	    //Connect timeout
	    clientHttpRequestFactory.setConnectTimeout(SOCKET_TIMEOUT_IN_MS);
	    //Read timeout
	    clientHttpRequestFactory.setReadTimeout(SOCKET_TIMEOUT_IN_MS);
	    return clientHttpRequestFactory;
	}
	private static String createArequest(String url,HttpMethod httpMethod,HttpHeaders headers ,String input,String cloudId) throws Exception {
		ResponseEntity<String> response = null;
		String result = null;
		try{
			URI uri = null;
			if(url!=null) {
				url  = UriUtils.encodeFragment(url, StandardCharsets.UTF_8);
				uri = new URI(url);
			}
			HttpEntity<?> entity = null;
			if(StringUtils.isEmpty(input)) {
				entity =  new HttpEntity<>(headers);
			}else {
				entity =  new HttpEntity<>(input,headers);
			}
			response = restTemplate2.exchange(
					uri,
					httpMethod,
					entity,
					String.class
					);

			result=response.getBody();
			//updateRateLimitPerHour(cloudId, false);
			if(response.getStatusCode().is4xxClientError() && response.getStatusCodeValue()!=429){
				log.info(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				throw new MailCreationException(MailExceptionUtils.FETCH_MAILS+result);
			}else if(response.getStatusCodeValue()==429) {
				log.info(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				//updateRateLimitPerHour(cloudId, true);
			}
			return result;
		}catch (Exception ex) {
			log.warn(ExceptionUtils.getStackTrace(ex));
			throw ex;
		}
	}


//	private static void updateRateLimitPerHour(String cloudId,boolean rateLimit) {
//		if(StringUtils.isNotBlank(cloudId)) {
//			Clouds cloud = dbConnectorService2.getCloudsRepoImpl().findOne(cloudId);
//			checkClouds(cloud);
//			cloud.setRateLimit(rateLimit);
//			cloud.setRCount(cloud.getRCount()+1);
//			if(cloud.getModifiedTime()==null) {
//				cloud.setModifiedTime(LocalDateTime.now());
//			}
//			if(rateLimit || cloud.isRateLimit()) {
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//				}
//				cloud.setRateLimit(rateLimit);
//			}
//			setRateLimitCount(cloud);
//			dbConnectorService2.getCloudsRepoImpl().save(cloud);
//		}
//	}


//	private static void setRateLimitCount(Clouds cloud) {
//		boolean reset  = false;
//		log.info("==Rate Limit Count=="+cloud.getEmail()+"==Type:-"+cloud.getRType().name()+"==Count:-"+cloud.getRCount());
//			if(cloud.getRType()==Clouds.RTYPE.HOUR) {
//				if(cloud.getModifiedTime().plusHours(1).isBefore(LocalDateTime.now()))
//					reset = true;
//			}else if(cloud.getRType()==Clouds.RTYPE.MINUTE){
//				if(cloud.getModifiedTime().plusMinutes(1).isBefore(LocalDateTime.now()))
//					reset = true;
//			}else {
//				if(cloud.getModifiedTime().plusDays(1).isBefore(LocalDateTime.now()))
//					reset = true;
//			}
//		if(reset) {
//			cloud.setModifiedTime(LocalDateTime.now());
//			cloud.setMetaUrl(String.valueOf(cloud.getRCount()));
//			cloud.setRCount(0);
//		}
//	}

	private static String createDownloadrequest(String uri,HttpMethod httpMethod,HttpHeaders headers ,String input,String cloudId) throws HttpStatusCodeException {
		ResponseEntity<String> response = null;
		String result = null;
		try{
			uri = UriUtils.decode(uri, StandardCharsets.UTF_8.name());
			HttpEntity<?> entity = null;
			if(StringUtils.isEmpty(input)) {
				entity =  new HttpEntity<>(headers);
			}else {
				entity =  new HttpEntity<>(input,headers);
			}
			response = restTemplate2.exchange(
					uri,
					httpMethod,
					entity,
					String.class
					);

			result=response.getBody();
			//updateRateLimitPerHour(cloudId, false);
			if(response.getStatusCode().is4xxClientError() && response.getStatusCodeValue() !=429){
				log.info(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				throw new MailCreationException(MailExceptionUtils.FETCH_MAILS+result);
			}else if(response.getStatusCodeValue()==429) {
				log.info(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				//updateRateLimitPerHour(cloudId, true);
			}
			return result;
		}catch (Exception ex) {
			log.warn(ExceptionUtils.getStackTrace(ex));
			throw ex;
		}
	}



	private static String createAFilerequest(String uri,HttpMethod httpMethod,HttpHeaders headers ,byte[] input,String cloudId) throws HttpStatusCodeException {
		ResponseEntity<String> response = null;
		String result = null;
		try{
			uri = UriUtils.decode(uri, StandardCharsets.UTF_8.name());
			HttpEntity<?> entity = null;
			if(input==null) {
				entity =  new HttpEntity<>(headers);
			}else {
				entity =  new HttpEntity<>(input,headers);
			}
			response = restTemplate2.exchange(
					uri,
					httpMethod,
					entity,
					String.class
					);

			result=response.getBody();
			//updateRateLimitPerHour(cloudId, false);
			if(response.getStatusCode().is4xxClientError() && response.getStatusCodeValue() !=429){
				log.warn(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				throw new MailCreationException(MailExceptionUtils.FETCH_MAILS+result);
			}else if(response.getStatusCodeValue()==429) {
				log.info(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				//updateRateLimitPerHour(cloudId, true);
			}else if(response.getStatusCode().is2xxSuccessful() && response.getHeaders().get("Location")!=null) {
				return response.getHeaders().get("Location").get(0);
			}else if(response.getStatusCode().is3xxRedirection() && response.getHeaders().get("range")!=null) {
				return response.getHeaders().get("range").get(0);
			}
			return result;
		}catch (Exception ex) {
			log.warn(ExceptionUtils.getStackTrace(ex));
			throw ex;
		}
	}



	private static String createAHeaderrequest(String uri,HttpMethod httpMethod,HttpHeaders headers ,String input,String cloudId) throws HttpStatusCodeException {
		ResponseEntity<String> response = null;
		String	result = null;
		try{
			uri = UriUtils.decode(uri, StandardCharsets.UTF_8.name());
			HttpEntity<?> entity = null;
			if(StringUtils.isEmpty(input)) {
				entity =  new HttpEntity<>(headers);
			}else {
				entity =  new HttpEntity<>(input,headers);
			}
			response = restTemplate2.exchange(
					uri,
					httpMethod,
					entity,
					String.class
					);

			//updateRateLimitPerHour(cloudId, false);
			if(response.getStatusCode().is4xxClientError() && response.getStatusCodeValue() !=429){
				log.warn(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				throw new MailCreationException(MailExceptionUtils.FETCH_MAILS+result);
			}else if(response.getStatusCodeValue()==429) {
				log.info(MailExceptionUtils.FETCH_MAILS + response.getStatusCode()+" result : "+result);
				//updateRateLimitPerHour(cloudId, true);
			}else if(response.getStatusCode().is2xxSuccessful()) {
				if(response.getHeaders().containsKey("Location")) {
					String location = response.getHeaders().get("Location").get(0);
					response = restTemplate2.exchange(
							location,
							HttpMethod.GET,
							entity,
							String.class
							);
					result = response.getBody();
				}else {
					result =response.getBody();
				}
			}

			return result;
		}catch (Exception ex) {
			log.warn(ExceptionUtils.getStackTrace(ex));
			throw ex;
		}
	}

	public static void randomWait(int n) {
		try {
			log.warn("==Making the thread to sleep for =="+n);
			Thread.sleep((long) (1 << n+1) * 1000 + randomGenerator.nextInt(1001));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}



	public static String postResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		if(cfoId!=null && null==accessToken) {
			accessToken = dbConnectorService2.getCredentialsRepo().findById(cfoId).getAccessToken();
		}
		for(int i =0;i<2;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				result = ConnectUtils.createArequest(url, HttpMethod.POST, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}
			}
			catch(Exception e) {
				try {
					Thread.sleep(i*2000);
				} catch (InterruptedException e1) {
				}
				log.warn(ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.POST_EXCEPTION, e);
				}else {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return result;
	}




	public static String postResponseFormValue(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String mime,long fileSize,String cloudID) throws MailMigrationException{
		String result=null;
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
				headers.set("Upload-Content-Type", mime);
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set("Upload-Content-Length",  String.format(Locale.ENGLISH, "%d",fileSize));
				headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
				headers.set(HttpHeaders.CONTENT_LENGTH, String.format(Locale.ENGLISH, "%d",input.getBytes("utf-8").length));
				result = ConnectUtils.createAFilerequest(url, HttpMethod.POST, headers, input.getBytes("utf-8"),cloudID);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudID);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}			}
			catch(Exception e) {
				log.warn(ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.POST_EXCEPTION, e);
				}else {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return result;
	}


	private static String exceptionHandler(String accessToken, String cfoId, CLOUD_NAME label, String result,
			int i, HttpClientErrorException e,String cloudId) {
		log.error(ExceptionUtils.getStackTrace(e));
		if(e.getStatusCode().value()==401) {
			accessToken =  verifyAccessToken(cfoId, true, label);
		}else if(e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS || e.getStatusCode().value() == 429) {
			log.warn("==THREAD WAITING ENABLED FOR THE STATUS=="+e.getStatusCode().value());
			ConnectUtils.randomWait(i);
			//updateRateLimitPerHour(cloudId, true);
			i=2;
			throw e;
		}else if(e.getStatusCode().value() == 400 && e.getMessage() != null && e.getMessage().contains("already exist")){
			return result;
		}else if(e.getStatusCode().value() == 503 && e.getMessage() != null && e.getMessage().contains("Mailbox move in progress")){
			i=2;
			throw e;
		}
		else {
			throw e;
		}
		return accessToken;
	}
	public static String putResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		if(cfoId!=null) {
			accessToken = dbConnectorService2.getCredentialsRepo().findById(cfoId).getAccessToken();
		}
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.CONTENT_LENGTH, String.format(Locale.ENGLISH, "%d",input.getBytes().length));
				result = ConnectUtils.createArequest(url, HttpMethod.PUT, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}}
			catch(Exception e) {
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.PUT_EXCEPTION, e);
				}else {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return result;
	}


	public static String putResponseStream(String url,String accessToken,byte[] input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		if(cfoId!=null) {
			accessToken = dbConnectorService2.getCredentialsRepo().findById(cfoId).getAccessToken();
		}
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.CONTENT_LENGTH, String.format(Locale.ENGLISH, "%d",input.length));
				result = ConnectUtils.createAFilerequest(url, HttpMethod.PUT, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}
			}
			catch(Exception e) {
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.PUT_EXCEPTION, e);
				}else {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return result;
	}



	public static String deleteResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		if(cfoId!=null) {
			accessToken = dbConnectorService2.getCredentialsRepo().findById(cfoId).getAccessToken();
		}
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				result = ConnectUtils.createArequest(url, HttpMethod.DELETE, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}
			}
			catch(Exception e) {
				log.error("--Iteration --"+i+"=="+ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.DELETE_EXCEPTION, e);
				}
			}
		}
		return result;
	}



	public static String getResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		if(cfoId!=null) {
			accessToken = dbConnectorService2.getCredentialsRepo().findById(cfoId).getAccessToken();
		} 
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				if(accessToken!=null && !accessToken.startsWith(BEARER)) {
					headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				}else {
					headers.set(HttpHeaders.AUTHORIZATION,accessToken);
				}
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				if(StringUtils.isNotBlank(input)) {
					headers.set("ConsistencyLevel", input);
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
				}else {
					headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				}
				result = ConnectUtils.createArequest(url, HttpMethod.GET, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}
			}catch(Exception e) {
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.GET_EXCEPTION, e);
				}
			}
		}
		return result;
	}



	public static String getFileDownloadResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				if(accessToken!=null && !accessToken.startsWith(BEARER)) {
					headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				}else {
					headers.set(HttpHeaders.AUTHORIZATION,accessToken);
				}
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				if(StringUtils.isNotBlank(input)) {
					headers.set("ConsistencyLevel", input);
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
				}else {
					headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				}
				result = ConnectUtils.createDownloadrequest(url, HttpMethod.GET, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){	
				log.error(ExceptionUtils.getStackTrace(e));
			}
			catch(Exception e) {
				log.error(url+":"+ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.GET_EXCEPTION, e);
				}
			}
		}
		return result;
	}





	public static InputStream executeDownloadRequest(String downloadUrl, String accessToken,  String fileId, String defaultDownloadUrl,String htmlUrl){
		InputStream inputStream = null;
		int maxRetries = 3;

		for (int n = 0; n < maxRetries; n++) {
			ClientResponse response = null;
			try {

				WebResource resource = client.resource(downloadUrl);
				response = resource.header("Authorization", "Bearer " +accessToken).get(ClientResponse.class);
				if(response.getStatus() == 200 || response.getStatus() == 201){
					inputStream = response.getEntity(InputStream.class);
					return inputStream;
				}else{
					if(response.getStatus() == 404 || response.getStatus()==400){
						log.warn("Exception occured while downlaoding Stream "+response.getStatus()+"==cloudId===adding Expot Link===");
						if(htmlUrl!=null){
							log.warn("==Export Link Url Added==="+"==url=="+htmlUrl);
							downloadUrl = htmlUrl;
						}else{
							downloadUrl = defaultDownloadUrl;
						}
					}
					try {
						Thread.sleep((1 << n) * 5000 + randomGenerator.nextInt(1001));
					} catch (InterruptedException e) {}
					try{
						if(n == maxRetries-1){
							String errorResult = response.getEntity(String.class);
							log.error(errorResult);
						}else{
							log.warn(String.format("Applying retry mechanism for GSuite Download for file %s  Cloud %s ", fileId,""));
						}
					}catch(Exception e){
						throw e;
					}
				}
			} catch (Exception e) {
				log.error("Exception occured while downlaoding Stream "+ExceptionUtils.getMessage(e)+"==cloudId====adding Expot Link===");
				if(response!=null && (response.getStatus() == 404 || response.getStatus()==400)){
					if(htmlUrl!=null){
						log.warn("==Export Link Url Added=====url=="+htmlUrl);
						downloadUrl = htmlUrl;
					}else{
						downloadUrl = defaultDownloadUrl;
					}
				}
				if(n == maxRetries-1){
					throw e;
				}
			} 
		}
		return inputStream;
	}




	public static String patchResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailCreationException{
		String result=null;
		if(cfoId!=null) {
			accessToken = dbConnectorService2.getCredentialsRepo().findById(cfoId).getAccessToken();
		}
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				result = ConnectUtils.createArequest(url, HttpMethod.PATCH, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				String  _accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
				if(null==_accessToken) {
					accessToken = _accessToken;
				}
			}catch(Exception e) {
				log.error(url+":"+ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.PATCH_EXCEPTION, e);
				}
			}
		}
		return result;
	}


	public static synchronized String verifyAccessToken(String cfoId,boolean immediateModify,CLOUD_NAME label) {
		String accessToken = null;
		switch (label) {
		case OUTLOOK: {
			accessToken =  refreshTokenMicroSoft(cfoId);
			break;
		}
		case GMAIL:{
			accessToken = verifyAccessTokenGoogle(cfoId, immediateModify);
			break;
		}

		default:
			throw new IllegalArgumentException("Unexpected value: " + label);
		}
		return accessToken;
	}


	public static String refreshTokenMicroSoft(String id) {
		if(id==null) {
			return null;
		}
		VendorOAuthCredential credential = dbConnectorService2.getCredentialsRepo().findById(id);
		OAuthKey keys =dbConnectorService2.getAppMongoOpsManager().findOAuthKeyByCloud(CLOUD_NAME.OUTLOOK);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "refresh_token");
		form.add("client_id", keys.getClientId());
		form.add("client_secret", keys.getClientSecret());
		form.add("refresh_token", credential.getRefreshToken());

		String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

		try {
			ResponseEntity<String> response = restTemplate2.exchange(url, HttpMethod.POST, entity, String.class);
			String result = response.getBody();
			RefreshTokenResult tokenResult = objMapper.readValue(result, RefreshTokenResult.class);
			if(tokenResult.getError()== null) {
				VendorOAuthCredential newCredential = convertToVendorCredential(tokenResult,CLOUD_NAME.OUTLOOK);
				newCredential.setId(id);
				newCredential.setLastRefreshed(LocalDateTime.now());
				dbConnectorService2.getCredentialsRepo().save(newCredential);
				return newCredential.getAccessToken();	
			} else {
				log.error(Thread.currentThread().getName() + " :Error refreshing access token, "+ tokenResult.toString());
			}
		} catch (RestClientException e) {
			log.warn("Request Failed while refreshing token  : " + ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (Exception e) {
			log.warn("Error while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}


	public static VendorOAuthCredential convertToVendorCredential(RefreshTokenResult tokenResult,CLOUD_NAME saaSVendorLabel) {
		VendorOAuthCredential credential = new VendorOAuthCredential();
		credential.setAccessToken(tokenResult.getAccessToken());
		credential.setRefreshToken(tokenResult.getRefreshToken());
		credential.setCloudName(saaSVendorLabel);
		credential.setLastRefreshed(LocalDateTime.now());
		credential.setExpiresAt(LocalDateTime.now().plusHours(1));
		return credential;
	}

	public static String verifyAccessTokenGoogle(String emailId,boolean retry) {
		if(StringUtils.isBlank(emailId)) {
			return null;
		}
		if(emailId.contains(":")) {
			emailId = emailId.split(":")[0];
		}
		VendorOAuthCredential oAuthCredential = dbConnectorService2.getCredentialsRepo().findById(emailId+":"+CLOUD_NAME.GMAIL.name());
		if(oAuthCredential!=null) {
			try {
				VendorOAuthCredential creds =  apiHelper2.getAccessTokenForUser(emailId, oAuthCredential.getRefreshToken());
				if(creds!=null && creds.getAccessToken()!=null && !oAuthCredential.getAccessToken().equals(creds.getAccessToken())) {
					oAuthCredential.setAccessToken(creds.getAccessToken());
					oAuthCredential.setLastRefreshed(LocalDateTime.now());
					oAuthCredential.setExpiresAt(creds.getExpiresAt());	
					dbConnectorService2.getCredentialsRepo().save(oAuthCredential);
					return oAuthCredential.getAccessToken();
				}
			} catch (Exception e) {
				log.warn(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	public static String  getHeaderResponse(String url,String accessToken,String input,String cfoId,CLOUD_NAME label,String cloudId) throws MailMigrationException{
		String result=null;
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.AUTHORIZATION,BEARER+accessToken);
				headers.set(HttpHeaders.USER_AGENT, USER_AGENT_VALUE);
				if(StringUtils.isNotBlank(input)) {
					headers.set("ConsistencyLevel", input);
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
				}else {
					headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
				}
				result = ConnectUtils.createAHeaderrequest(url, HttpMethod.GET, headers, input,cloudId);
				return result;
			}catch(HttpClientErrorException e){	
				accessToken = exceptionHandler(accessToken, cfoId, label, result, i, e,cloudId);
			}
			catch(Exception e) {
				log.error(url+":"+ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.GET_EXCEPTION, e);
				}
			}
		}
		return result;
	}

	public static void checkClouds(Clouds clouds) {
		if(clouds==null || !clouds.isActive()) {
			throw new MailMigrationException(ExceptionConstants.CLOUD_NOT_FOUND);
		}
		if(clouds.isRateLimit()) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
		}
	}

	public static String uploadSession(String url,byte[] f,String conenteRange,int limit,String cloudId) throws MailCreationException{
		String result=null;
		for(int i =0;i<3;i++) {
			try{
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(limit));
				headers.set(HttpHeaders.CONTENT_RANGE, conenteRange);
				result = ConnectUtils.createAFilerequest(url, HttpMethod.PUT, headers, f,cloudId);
				return result;
			}catch(HttpClientErrorException e){
				log.warn(ExceptionUtils.getStackTrace(e));
			}
			catch(Exception e) {
				log.error(url+":"+ExceptionUtils.getStackTrace(e));
				if(i==2) {
					throw new MailCreationException(MailExceptionUtils.PATCH_EXCEPTION, e);
				}
			}
		}
		return result;
	}
}
