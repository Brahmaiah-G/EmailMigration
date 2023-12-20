package com.cloudfuze.mail.connectors.impl.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.cloudfuze.mail.connectors.google.data.AccessTokenRequest;
import com.cloudfuze.mail.connectors.google.data.AccessTokenResponse;
import com.cloudfuze.mail.connectors.google.data.Attachment;
import com.cloudfuze.mail.connectors.google.data.Attendee;
import com.cloudfuze.mail.connectors.google.data.Biography;
import com.cloudfuze.mail.connectors.google.data.Connection;
import com.cloudfuze.mail.connectors.google.data.EmailAddress;
import com.cloudfuze.mail.connectors.google.data.EntryPoint;
import com.cloudfuze.mail.connectors.google.data.EventItem;
import com.cloudfuze.mail.connectors.google.data.EventsList;
import com.cloudfuze.mail.connectors.google.data.Filter;
import com.cloudfuze.mail.connectors.google.data.Group;
import com.cloudfuze.mail.connectors.google.data.Header;
import com.cloudfuze.mail.connectors.google.data.Label;
import com.cloudfuze.mail.connectors.google.data.MailValue;
import com.cloudfuze.mail.connectors.google.data.Organization;
import com.cloudfuze.mail.connectors.google.data.Part;
import com.cloudfuze.mail.connectors.google.data.PhoneNumber;
import com.cloudfuze.mail.connectors.google.data.SendAs;
import com.cloudfuze.mail.connectors.management.utility.ConnectorUtility;
import com.cloudfuze.mail.connectors.microsoft.data.AttachmentsData;
import com.cloudfuze.mail.connectors.microsoft.data.RefreshTokenResult;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.contacts.entities.Address;
import com.cloudfuze.mail.contacts.entities.Contacts;
import com.cloudfuze.mail.contacts.entities.Emails;
import com.cloudfuze.mail.contacts.entities.PhoneNumbers;
import com.cloudfuze.mail.contacts.utils.CountryCodes;
import com.cloudfuze.mail.dao.entities.CalenderFlags;
import com.cloudfuze.mail.dao.entities.EMailRules;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.dao.entities.EmailUserSettings;
import com.cloudfuze.mail.dao.entities.UserGroups;
import com.cloudfuze.mail.dao.impl.AppMongoOpsManager;
import com.cloudfuze.mail.repo.entities.CalenderInfo;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.entities.MemberDetails;
import com.cloudfuze.mail.repo.entities.OAuthKey;
import com.cloudfuze.mail.repo.entities.VendorOAuthCredential;
import com.cloudfuze.mail.repo.impl.CloudsRepoImpl;
import com.cloudfuze.mail.repo.impl.VendorOAuthCredentialImpl;
import com.cloudfuze.mail.utils.EmailUtils;
import com.cloudfuze.mail.utils.EventRangeUtils;
import com.cloudfuze.mail.utils.HttpUtils;
import com.cloudfuze.mail.utils.MappingUtils;
import com.cloudfuze.mail.utils.MappingUtils.MAIL_FOLDERS;
import com.cloudfuze.mail.utils.TimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GmailHelper {


	@Autowired
	private AppMongoOpsManager appMongoOpsManager;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private VendorOAuthCredentialImpl authCredentialImpl;

	private static ObjectMapper objectMapper = new ObjectMapper();


	private String FETCH_ACCESS_TOKEN_URL = "https://oauth2.googleapis.com/token";

	private String REVOKE_ACCESS_TOKEN = "https://oauth2.googleapis.com/revoke";

	@Autowired
	CloudsRepoImpl cloudsRepoImpl;

	public GmailHelper() {
		loadDays();
	}


	private String REFRESH_TOKEN  ="refresh_token";
	static Map<String,String> eventDays = new HashMap<>();
	Map<String,String>TIMEZONE_MAPPINGS = TimeUtils.loadTimeZones();
	public static Map<String,String> loadDays() {
		eventDays.put("SU", "sunday");
		eventDays.put("MO", "monday");
		eventDays.put("TU", "tuesday");
		eventDays.put("WE", "wednesday");
		eventDays.put("TH", "thursday");
		eventDays.put("FR", "friday");
		eventDays.put("SA", "saturday");
		eventDays.put("sunday", "SU");
		eventDays.put("monday", "MO");
		eventDays.put("tuesday", "TU");
		eventDays.put("wednesday", "WE");
		eventDays.put("thursday", "TH");
		eventDays.put("friday", "FR");
		eventDays.put("saturday", "SA");
		return eventDays;

	}


	public VendorOAuthCredential getAccesstoken(String authorizationCode) {

		log.info("Fetching Access Token");
		VendorOAuthCredential oAuthcreds = null;

		AccessTokenRequest requestToken = getTokenRequest(1,authorizationCode);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", requestToken.getClientId());
		form.add("client_secret", requestToken.getClientSecret());
		form.add("redirect_uri", requestToken.getRedirectURL());
		form.add("code", requestToken.getAuthCode());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form,headers);
		AccessTokenResponse  token = null;
		ResponseEntity<AccessTokenResponse> response=null;				
		try {    	
			response = restTemplate.exchange(FETCH_ACCESS_TOKEN_URL, HttpMethod.POST,entity, AccessTokenResponse.class);
		} 
		catch(HttpStatusCodeException hsce) {

			response = ResponseEntity.status(hsce.getRawStatusCode()).eTag(hsce.getResponseBodyAsString()).build();
		}
		catch (Exception e) {
			log.error(e.getMessage());			
		}

		if(response!=null && response.getStatusCode().is2xxSuccessful()) {

			token = response.getBody();
			oAuthcreds = getCredsfromToken(token);

		} 


		return oAuthcreds;
	}

	private VendorOAuthCredential refreshAccessToken(VendorOAuthCredential oAuthcreds) {

		VendorOAuthCredential newTokencreds = null;

		AccessTokenRequest requestToken = getTokenRequest(2,oAuthcreds.getRefreshToken());
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", REFRESH_TOKEN);
		form.add("client_id", requestToken.getClientId());
		form.add("client_secret", requestToken.getClientSecret());
		form.add("redirect_uri", requestToken.getRedirectURL());
		form.add(REFRESH_TOKEN, requestToken.getRefreshToken());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form,headers);
		AccessTokenResponse  token = null;

		try {

			ResponseEntity<AccessTokenResponse> response=null;				
			try {    	
				response = restTemplate.exchange(FETCH_ACCESS_TOKEN_URL, HttpMethod.POST, entity, AccessTokenResponse.class);
			} 
			catch(HttpStatusCodeException hsce) {

				response = ResponseEntity.status(hsce.getRawStatusCode()).eTag(hsce.getResponseBodyAsString()).build();
			}

			if(response.getStatusCode().is2xxSuccessful()) {

				token = response.getBody();
				newTokencreds = getCredsfromToken(token);

			}	

		} 
		catch (RestClientException e) {
			log.error(e.getMessage());			
		}
		catch (Exception e) {
			log.error(e.getMessage());			
		}

		return newTokencreds;
	}


	private Boolean hasTokenExpired(VendorOAuthCredential oAuthcreds) {		
		if(oAuthcreds.getExpiresAt()!=null) {
			return !oAuthcreds.getExpiresAt().isAfter(LocalDateTime.now());
		}else {
			return true;
		}
	}

	public VendorOAuthCredential verifyAccessToken(VendorOAuthCredential oAuthcreds,boolean retry) {

		Boolean isTokenExpired = hasTokenExpired(oAuthcreds);

		if(isTokenExpired ==  Boolean.FALSE && !retry)
			return oAuthcreds;
		else 		
			return refreshAccessToken(oAuthcreds);
	}


	private AccessTokenRequest getTokenRequest(Integer type,String code) {
		OAuthKey keys = appMongoOpsManager.findOAuthKeyByCloud(CLOUD_NAME.GMAIL);
		AccessTokenRequest requestToken = new AccessTokenRequest();
		requestToken.setClientId(keys.getClientId());
		requestToken.setClientSecret(keys.getClientSecret());
		requestToken.setRedirectURL(keys.getRedirectUrl());

		switch(type){

		case 1:
			requestToken.setGrant_type("authorization_code");
			requestToken.setAuthCode(code);
			break;

		case 2:
			requestToken.setGrant_type(REFRESH_TOKEN);
			requestToken.setRefreshToken(code);
			break;

		default:				
			break;
		}
		return requestToken;
	}

	private Boolean revokeAccessToken(VendorOAuthCredential oAuthcreds) {

		Boolean success = Boolean.FALSE;

		String url1 = UriComponentsBuilder.fromHttpUrl(REVOKE_ACCESS_TOKEN).queryParam("token", oAuthcreds.getAccessToken()).build().toUriString();	
		String url2 = UriComponentsBuilder.fromHttpUrl(REVOKE_ACCESS_TOKEN).queryParam("token", oAuthcreds.getRefreshToken()).build().toUriString();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<AccessTokenRequest> requestEntity = new HttpEntity<>(null, headers);

		try {

			ResponseEntity<String> response=null;
			try {    	
				response = restTemplate.exchange(url1, HttpMethod.POST, requestEntity, String.class);

				if( response.getStatusCode().is2xxSuccessful()) {
					success = Boolean.TRUE;
				}

			} 
			catch(HttpStatusCodeException hsce) {
				success = Boolean.FALSE;
				response =  ResponseEntity.status(hsce.getRawStatusCode())
						.headers(hsce.getResponseHeaders())
						.body(hsce.getResponseBodyAsString());
			}

			if(response.getStatusCode().is4xxClientError()) {

				try {    	
					response = restTemplate.exchange(url2, HttpMethod.POST, requestEntity, String.class);
				} 
				catch(HttpStatusCodeException hsce) {

					response = ResponseEntity.status(hsce.getRawStatusCode())
							.headers(hsce.getResponseHeaders())
							.body(hsce.getResponseBodyAsString());
				}
				if(response.getStatusCode().is2xxSuccessful()) {
					success = Boolean.TRUE;
				}
				if(response.getStatusCode().is4xxClientError()) {
					success = Boolean.FALSE;
				}

			} 				

		} 
		catch (RestClientException e) {
			log.error(e.getMessage());			
		}
		catch (Exception e) {
			log.error(e.getMessage());			
		}		

		return success;
	}	

	public VendorOAuthCredential getCredsfromToken(AccessTokenResponse token) {

		VendorOAuthCredential newTokencreds = new VendorOAuthCredential();
		newTokencreds.setAccessToken(token.getAccessToken());
		newTokencreds.setRefreshToken(token.getRefreshToken());
		newTokencreds.setLastRefreshed(LocalDateTime.now());
		newTokencreds.setExpiresAt(LocalDateTime.now());
		newTokencreds.setCloudName(CLOUD_NAME.GMAIL);

		return newTokencreds;
	}

	public VendorOAuthCredential getAccessTokenForUser(String emailId,String clientEmail) throws Exception {
		try {
			Client client = Client.create();
			String url = "https://www.googleapis.com/oauth2/v4/token";
			String accessToken=null;
			GmailJWTAssertion gsuiteJWTAssertion =new GmailJWTAssertion();
			String assertion = gsuiteJWTAssertion.createJWT(emailId, clientEmail);
			VendorOAuthCredential credential = new VendorOAuthCredential();
			MultivaluedMap formData = new MultivaluedMapImpl();
			log.warn("emailId : "+emailId+" clientEmail : "+clientEmail);
			formData.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
			formData.add("assertion", assertion);
			WebResource resource = client.resource(url);
			ClientResponse response = resource.header("Content-Type","application/x-www-form-urlencoded").post(ClientResponse.class, formData);
			String result = response.getEntity(String.class);
			log.warn(result);
			try{
				RefreshTokenResult tokenResult = objectMapper.readValue(result, RefreshTokenResult.class);
				if(StringUtils.isNotBlank(tokenResult.getAccessToken())){
					accessToken=tokenResult.getAccessToken();
					credential.setAccessToken(accessToken);
					credential.setRefreshToken(clientEmail);
					credential.setLastRefreshed(LocalDateTime.now());
					credential.setExpiresAt(LocalDateTime.now().plusHours(1));
					credential.setId(emailId+":"+CLOUD_NAME.GMAIL);
					credential.setCloudName(CLOUD_NAME.GMAIL);
					authCredentialImpl.save(credential);
					log.warn("accessToken : "+accessToken);
					return credential;
				}else {
					throw new Exception(result);
				}
			}catch(Exception e){
				log.warn(ExceptionUtils.getStackTrace(e));
				throw e;
			}
		}  catch (Exception e) {
			throw e;
		}
	}

	public String createBodyForSendAsSettings(EmailUserSettings emailUserSettings) {
		JSONObject json = new JSONObject();
		json.put("sendAsEmail", emailUserSettings.getEmail());
		json.put("displayName", emailUserSettings.getDisplayName());
		json.put("replyToAddress", emailUserSettings.getReplyToAddress());
		json.put("signature", emailUserSettings.getSignature());
		json.put("verificationStatus", emailUserSettings.getVerificationStatus());
		json.put("treatAsAlias", emailUserSettings.isAlias());
		return json.toString();
	}

	public EmailUserSettings setEmailSettings(SendAs sendAs) {
		EmailUserSettings emailSettings = new EmailUserSettings();
		emailSettings.setSendAs(true);
		emailSettings.setEmail(sendAs.getSendAsEmail());
		emailSettings.setSignature(sendAs.getSignature());
		emailSettings.setReplyToAddress(sendAs.getReplyToAddress());
		emailSettings.setPrimary(sendAs.isPrimary());
		emailSettings.setDefault(sendAs.isDefault());
		emailSettings.setDisplayName(sendAs.getDisplayName());
		emailSettings.setAlias(sendAs.isTreatAsAlias());
		emailSettings.setVerificationStatus(sendAs.getVerificationStatus());
		return emailSettings;
	}

	public String createBodyForRule(EMailRules eMailRules ) {
		JSONObject rule = new JSONObject();
		JSONObject action = new JSONObject();
		JSONObject criteria = new JSONObject();
		JSONArray lableIds = new JSONArray();
		JSONArray removeLableIds = new JSONArray();
		if(eMailRules.getFromAddresses()!=null && !eMailRules.getFromAddresses().isEmpty()) {
			criteria.put("from", eMailRules.getFromAddresses().toString().substring(1, eMailRules.getFromAddresses().toString().length()-1));
		}
		criteria.put("hasAttachment", eMailRules.isAttachments());
		if(eMailRules.getSentToAddresses()!=null && !eMailRules.getSentToAddresses().isEmpty()) {
			criteria.put("to", eMailRules.getSentToAddresses().toString().substring(1, eMailRules.getSentToAddresses().toString().length()-1));
		}
		if(eMailRules.getQuery()!=null) {
			criteria.put("query", eMailRules.getQuery());
		}
		if(eMailRules.getForwards()!=null && !eMailRules.getForwards().isEmpty()) {
			action.put("from", eMailRules.getForwards().toString().substring(1, eMailRules.getForwards().toString().length()-1));
		}
		if(eMailRules.isMarkAsRead()) {
			removeLableIds.put("UNREAD");
		}
		if(eMailRules.isMarkImportance()) {
			lableIds.put("IMPORTANT");
		}
		if(eMailRules.getRemoveLables()!=null && !eMailRules.getRemoveLables().isEmpty()) {
			removeLableIds.put(String.join(",", eMailRules.getRemoveLables()));
		}
		if(eMailRules.isDelete()) {
			lableIds.put("TRASH");
		}
		action.put("addLabelIds", lableIds);
		action.put("removeLabelIds", removeLableIds);
		if(lableIds.isEmpty() && removeLableIds.isEmpty()) {
			return null;
		}
		rule.put("action", action);
		rule.put("criteria", criteria);
		return rule.toString();
	}

	public EventRangeUtils setRange(CalenderFlags emailFlagsInfo) {
		//For the recurrence range getting 
		//Range in the form of  Type#startDate#endDate#FirstDay#WeekDays#interval from Outlook
		EventRangeUtils eventRangeUtils = new EventRangeUtils();
		if(emailFlagsInfo.getRange()!=null) {
			List<String> ranges = Arrays.asList(emailFlagsInfo.getRange().split(Const.HASHTAG));
			eventRangeUtils.setType(ranges.get(0));
			eventRangeUtils.setStartDate(ranges.get(1));
			if(eventRangeUtils.getType().equalsIgnoreCase("noEnd") && (ranges.get(4)!=null && !ranges.get(4).equals("null"))) {
				if(ranges.get(4).matches(".*\\d+.*")) {
					String number = ranges.get(4).replaceAll("[^0-9]", "");
					if(StringUtils.isNotBlank(number)) {
						eventRangeUtils.setIndex(number);
						ranges.set(4, ranges.get(4).replace(number, ""));
					}
				}
			}else if(ranges.get(4)!=null && !ranges.get(4).equals("null")) {
				if(ranges.get(4).matches(".*\\d+.*")) {
					String number = ranges.get(4).replaceAll("[^0-9]", "");
					if(StringUtils.isNotBlank(number)) {
						eventRangeUtils.setIndex(number);
						ranges.set(4, ranges.get(4).replace(number, ""));
					}
				}
			}
			eventRangeUtils.setEndDate(ranges.get(2));
			eventRangeUtils.setWkst(ranges.get(3));
			eventRangeUtils.setDays(ranges.get(4));
			eventRangeUtils.setOccurences(ranges.get(5));
			eventRangeUtils.setInterval(ranges.get(6));
		}
		return eventRangeUtils;
	}

	public String createRecurrence(CalenderFlags calenderFlags) {
		EventRangeUtils eventRange = setRange(calenderFlags);
		String rec = calenderFlags.getRecurrenceType();
		if(rec.toLowerCase().contains("monthly")) {
			rec = "MONTHLY";
		}else if(rec.toLowerCase().contains("yearly")) {
			rec = "YEARLY";
		}else if(rec.toLowerCase().contains("weekly")) {
			rec = "WEEKLY";
		}else {
			rec = "DAILY";
		}
		String range = "RRULE:FREQ="+rec.toUpperCase()+";";
		if(eventRange.getWkst()!=null && !eventRange.getWkst().equals("null")) {
			range = range+"WKST="+eventDays.get(eventRange.getWkst())+";";
		}
		if(eventRange.getType().equalsIgnoreCase("endDate")) {
			range = range+"UNTIL="+eventRange.getEndDate().replace("-", "")+";";
		}
		if(eventRange.getDays()!=null && !eventRange.getDays().isEmpty() && !eventRange.getDays().equals("null")) {
			if(calenderFlags.getRecurrenceType()!=null && calenderFlags.getRecurrenceType().equalsIgnoreCase("daily") && eventRange.getType().equalsIgnoreCase("noend") && eventRange.getDays().equals("null")) {
				String days = "[friday,monday,tuesday,wednesday,thursday,saturday]";
				eventRange.setDays(days);
			}
			List<String> byDays = Arrays.asList(eventRange.getDays().startsWith("[")?eventRange.getDays().substring(1, eventRange.getDays().length()-1).split(",") :eventRange.getDays().split(","));
			String _days= "";
			for(String day : byDays) {
				if(eventDays.get(day.trim())==null) {
					_days =_days+day.trim()+",";
				}else {
					_days = _days+eventDays.get(day.trim())+",";
				}
			}
			if(eventRange.getIndex()!=null && !eventRange.getIndex().equals("null")) {
				range = range+"BYDAY="+Integer.valueOf(eventRange.getIndex())+_days+";";
			}else {
				range = range+"BYDAY="+_days+";";
			}
		}
		if(eventRange.getInterval()!=null && !eventRange.getInterval().equalsIgnoreCase("null") && !eventRange.getInterval().isEmpty()) {
			int interval = 1;
			if(eventRange.getInterval().equals("second") || eventRange.getInterval().equals("2")) {
				interval = 2;
			}else if(eventRange.getInterval().equals("third") || eventRange.getInterval().equals("3")) {
				interval = 3;
			}else if(eventRange.getInterval().equals("fourth") || eventRange.getInterval().equals("4")) {
				interval = 4;
			}
			range = range+"INTERVAL="+interval+";";
		}
		if(eventRange.getOccurences()!=null && !eventRange.getOccurences().equals("null")) {
			// need to check for all the numbers if had or not for source as outlook
			String interval = eventRange.getOccurences();
			if(eventRange.getOccurences().equals("second") || eventRange.getOccurences().equals("2")) {
				interval = "2";
			}else if(eventRange.getOccurences().equals("third") || eventRange.getOccurences().equals("3")) {
				interval = "3";
			}else if(eventRange.getOccurences().equals("fourth") || eventRange.getOccurences().equals("4")) {
				interval = "4";
			}
			range = range+"COUNT="+Long.valueOf(interval);
		}

		return range;
	}

	public String createBodyForAttachment(EmailFlagsInfo emailFlagsInfo) {
		//Body For Attachments
		JSONArray array = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
			for(AttachmentsData data : emailFlagsInfo.getAttachments()) {
				JSONObject attach = new JSONObject();
				attach.put("fileId", data.getId());
				attach.put("mimeType", data.getContentType());
				attach.put("title", data.getName());
				attach.put("fileUrl", data.getOdataType());
				array.put(attach);
			}
			jsonObject.put("attachments", array);
			JSONObject end = new JSONObject();
			end = emailFlagsInfo.getStartTime().split("T").length>1? end.put("dateTime", emailFlagsInfo.getStartTime()): end.put("date", emailFlagsInfo.getStartTime());
			end.put("timeZone", emailFlagsInfo.getTimeZone());
			jsonObject.put("start", end);
			JSONObject start = new JSONObject();
			start = emailFlagsInfo.getSentTime().split("T").length>1? end.put("dateTime", emailFlagsInfo.getSentTime()): end.put("date", emailFlagsInfo.getSentTime());
			start.put("timeZone", emailFlagsInfo.getEndTimeZone());
			jsonObject.put("end", start);
		}
		return jsonObject.toString();
	}


	/**
	 * For creating the calendar info from the events,
	 *  for delta use the <b>synctoken</b> In the response
	 */
	public CalenderInfo createCalendarInfo(EventItem calendar,Clouds cloud,EventsList emailFolders){
		CalenderInfo calenderInfo = new CalenderInfo();
		calenderInfo.setSubject(calendar.getSummary());
		calenderInfo.setSourceId(calendar.getId());
		calenderInfo.setId(calendar.getId());
		if(calendar.getStatus()!=null && calendar.getStatus().equalsIgnoreCase("cancelled")) {
			calenderInfo.setDeleted(true);
			return calenderInfo;
		}
		calenderInfo.setCalenderCreatedTime(calendar.getCreated());
		calenderInfo.setCalenderModifiedTime(calendar.getUpdated());
		calenderInfo.setOrganizer(calendar.getOrganizer().getEmail());
		calenderInfo.setStartTime(calendar.getStart().getDateTime()==null?calendar.getStart().getDate():calendar.getStart().getDateTime());
		if(emailFolders!=null) {
			calenderInfo.setTimeZone(calendar.getStart().getTimeZone()==null ? emailFolders.getTimeZone() : calendar.getStart().getTimeZone());
			if(emailFolders.getDefaultReminders()!=null && !emailFolders.getDefaultReminders().isEmpty()) {
				calenderInfo.setRemainderTime(emailFolders.getDefaultReminders().get(0).getMinutes());
			}
		}else {
			calenderInfo.setTimeZone(calendar.getStart().getTimeZone());
			if(calendar.getReminders()!=null) {
				//calenderInfo.setRemainderTime(calendar.getReminders().);
			}
		}
		calenderInfo.setUid(calendar.getRecurringEventId());
		calenderInfo.setICalUId(calendar.getICalUID());
		calenderInfo.setEndTime(calendar.getEnd().getDateTime()==null ?calendar.getEnd().getDate() : calendar.getEnd().getDateTime());
		calenderInfo.setBodyPreview(calendar.getSummary());
		calenderInfo.setEndTimeZone(calendar.getEnd().getTimeZone()!=null ?calendar.getEnd().getTimeZone():calenderInfo.getTimeZone());
		calenderInfo.setHtmlBodyContent(calendar.getDescription()==null?calendar.getSummary() : calendar.getDescription());
		calenderInfo.setHtmlBodyContent(StringEscapeUtils.unescapeHtml3(calenderInfo.getHtmlBodyContent()));
		calenderInfo.setColor(calendar.getColorId());
		if(calendar.getConferenceData()!=null && calendar.getConferenceData().getEntryPoints()!=null) {
			for(EntryPoint entryPoint : calendar.getConferenceData().getEntryPoints()) {
				if(entryPoint.getEntryPointType()!=null && entryPoint.getEntryPointType().equals("video")) {
					calenderInfo.setOnlineMeeting(true);
					calenderInfo.setOnlineMeetingUrl(calendar.getConferenceData().getConferenceId()+Const.HASHTAG+entryPoint.getUri()+Const.HASHTAG+calendar.getConferenceData().getConferenceSolution().getKey().getType());
					break;
				}
			}
		}
		if(calendar.getAttachments()!=null && !calendar.getAttachments().isEmpty()) {
			List<String> attachs = new ArrayList<>();
			calenderInfo.setAttachMents(true);
			for(Attachment attach : calendar.getAttachments()) {
				attachs.add(attach.getFileId()+":"+attach.getTitle()+":"+attach.getMimeType()+":"+attach.getFileUrl());
			}
			calenderInfo.setAttachmentIds(attachs);
		}
		if(calendar.getRecurrence()!=null) {
			String recur = calendar.getRecurrence().get(0);
			Map<String, String> rruleMap = new HashMap<>();
			String[] rruleParts = recur.split(";");

			for (String rrulePart : rruleParts) {
				try {
					String[] keyValue = rrulePart.split("=");
					if(keyValue.length>1) {
						rruleMap.put(keyValue[0], keyValue[1]);
					}else {
						log.info("===*****EMPTY RULE+++++++++++"+keyValue);
						rruleMap.put(keyValue[0],"");
					}
				} catch (Exception e) {
				}
			}

			if(StringUtils.isNotBlank(recur)) {
				calenderInfo.setRecurrenceType(rruleMap.get("RRULE:FREQ"));
				String endTime = null;
				String startTime =  calenderInfo.getStartTime().split("T")[0];
				String days = null;
				String range = null;
				String count = null;
				String type = "endDate";
				String interval = null;
				String wkst = "";
				if(rruleMap.containsKey("UNTIL")) {
					try {
						LocalDateTime	untilLocalDateTime = LocalDateTime.parse(rruleMap.get("UNTIL"), java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
						endTime = untilLocalDateTime.toString().split("T")[0];
					} catch (Exception e) {
						try {
							LocalDate	untilLocalDateTime = LocalDate.parse(rruleMap.get("UNTIL"), java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
							endTime = untilLocalDateTime.toString();
						} catch (Exception e1) {
						}
					}
				}

				if(rruleMap.containsKey("BYDAY")) {
					if(eventDays.size()==0) {
						loadDays();
					}
					days = rruleMap.get("BYDAY");
				}
				if(rruleMap.containsKey("INTERVAL")) {
					interval = rruleMap.get("INTERVAL");
				}
				if(rruleMap.containsKey("COUNT")) {
					count = rruleMap.get("COUNT");
				}
				if(rruleMap.containsKey("WKST")) {

					for(String day : Arrays.asList(rruleMap.get("WKST").split(","))) {
						wkst = wkst+eventDays.get(day)+",";
					}
					wkst = wkst.substring(0, wkst.length()-1);
				}else {
					wkst = null;
				}

				if(StringUtils.isBlank(endTime) || endTime == null) {
					type = "noEnd";
				}
				if(days!=null) {
					String number = days.replaceAll("[^0-9]", "");
					days = days.replace(number, "");
					List<String> byDays = Arrays.asList(days.split(","));
					String _days= "";
					for(String day : byDays) {
						_days = _days+eventDays.get(day)+",";
					}
					if(StringUtils.isNotBlank(_days)) {
						days = _days.substring(0, _days.length()-1);
					}
					if(StringUtils.isNotBlank(number)) {
						days = number+days;
					}
				}
				//Added in the form of type+HASHTAG+startTime+HASHTAG+endTime+HASHTAG+wkst+HASHTAG+days+HASHTAG+count+HASHTAG+interval
				range = type+Const.HASHTAG+startTime+Const.HASHTAG+endTime+Const.HASHTAG+wkst+Const.HASHTAG+days+Const.HASHTAG+count+Const.HASHTAG+interval+Const.HASHTAG;
				calenderInfo.setRange(range);
			}
		}
		calenderInfo.setReminderOn(calendar.getReminders().getUseDefault());
		if(calendar.getReminders().getOverrides()!=null) {
			calenderInfo.setRemainders(calendar.getReminders().getOverrides().stream()
					.map(override -> override.getMethod() + Const.HASHTAG + override.getMinutes())
					.collect(Collectors.joining(",")));
		}
		if(calendar.getLocation()!=null) {
			calenderInfo.setLocation(Arrays.asList(calendar.getLocation()));
		}
		calenderInfo.setVisibility(calendar.getEventType());
		if(calendar.getAttendees()!=null && !calendar.getAttendees().isEmpty()) {
			List<String>attendees = new ArrayList<>();
			for(Attendee attendee : calendar.getAttendees()) {
				attendees.add(attendee.getEmail()+":"+attendee.getResponseStatus()+":"+(attendee.isOptional()?"optional":"required")+":"+attendee.getComment());
			}
			calenderInfo.setAttendees(attendees);
		}
		return calenderInfo;
	}

	public String createBodyForEvent(CalenderFlags calenderFlags,List<AttachmentsData>attachMents,List<String>attendes) {
		JSONObject event = new JSONObject();
		JSONObject end = new JSONObject();
		if(calenderFlags.getEndTime().length()>26) {
			end = calenderFlags.getEndTime().split("T").length>1?end.put("dateTime", TimeUtils.convertOutlookTimeFormatWithOffset(calenderFlags.getEndTime(),TIMEZONE_MAPPINGS.containsKey(calenderFlags.getEndTimeZone())?TIMEZONE_MAPPINGS.get(calenderFlags.getEndTimeZone()):calenderFlags.getEndTimeZone())):end.put("date", calenderFlags.getEndTime());
		}else {
			end = calenderFlags.getEndTime().split("T").length>1?end.put("dateTime", calenderFlags.getEndTime()):end.put("date", calenderFlags.getEndTime());
		}
		end.put("timeZone", TIMEZONE_MAPPINGS.containsKey(calenderFlags.getEndTimeZone())?TIMEZONE_MAPPINGS.get(calenderFlags.getEndTimeZone()):calenderFlags.getEndTimeZone());
		event.put("end", end);
		JSONObject start = new JSONObject();
		if(calenderFlags.getStartTime().length()>26) {
			start = calenderFlags.getStartTime().split("T").length>1?start.put("dateTime", TimeUtils.convertOutlookTimeFormatWithOffset(calenderFlags.getStartTime(),TIMEZONE_MAPPINGS.containsKey(calenderFlags.getTimeZone())?TIMEZONE_MAPPINGS.get(calenderFlags.getTimeZone()):calenderFlags.getTimeZone())) :start.put("date", calenderFlags.getStartTime());
		}else {
			start = calenderFlags.getStartTime().split("T").length>1?start.put("dateTime", calenderFlags.getStartTime()) :start.put("date", calenderFlags.getStartTime());
		}
		start.put("timeZone", TIMEZONE_MAPPINGS.containsKey(calenderFlags.getTimeZone())?TIMEZONE_MAPPINGS.get(calenderFlags.getTimeZone()):calenderFlags.getTimeZone());
		event.put("start", start);
		JSONArray attendees = new JSONArray();
		if(calenderFlags.getAttendees()!=null && !calenderFlags.getAttendees().isEmpty()) {
			List<String>dups = new ArrayList<>();
			for(String attend : attendes) {
				if(dups.contains(attend.split(":")[0])) {
					continue;
				}
				dups.add(attend.split(":")[0]);
				JSONObject attendee = new JSONObject();
				attendee.put("email", attend.split(":")[0]);
				if(attend.split(":").length>1) {
					String status = EmailUtils.NEEDS_ACTION;
					String responseStatus = attend.split(":")[1];
					if(responseStatus.equalsIgnoreCase("tentativelyAccepted") || responseStatus.equalsIgnoreCase("tentative")) {
						status = "tentative";
					}else if(EmailUtils.ACCEPTED.equalsIgnoreCase(responseStatus)) {
						status = EmailUtils.ACCEPTED;
					}else if(EmailUtils.DECLINED.equalsIgnoreCase(responseStatus)) {
						status = EmailUtils.DECLINED;
					}
					try {
						attendee.put("optional", "optional".equals(attend.split(":")[2]));
					} catch (Exception e) {
					}

					attendee.put("responseStatus", status);
				}
				attendees.put(attendee);
			}
		}
		event.put("attendees", attendees);
		String iCalUid = calenderFlags.getICalUId()==null?RandomStringUtils.random(26, true, true)+"@google.com":calenderFlags.getICalUId();
		event.put("iCalUID", iCalUid);
		JSONObject organizer = new JSONObject();
		organizer.put("email", calenderFlags.getOrganizer());
		organizer.put("displayName", calenderFlags.getFromName());
		event.put("organizer", organizer);
		if(calenderFlags.isOnlineMeeting()) {
			calenderFlags.setHtmlMessage(calenderFlags.getHtmlMessage().replace(calenderFlags.getOnlineMeetingUrl(), ""));
		}
		event.put("description", calenderFlags.getHtmlMessage());
		event.put("summary", calenderFlags.getSubject());
		event.put("colorId", calenderFlags.getColour()==null?"1":calenderFlags.getColour());
		if(calenderFlags.getLocation()!=null && !calenderFlags.getLocation().isEmpty() ) {
			event.put("location", calenderFlags.getLocation().get(0).split(":")[0]);
		}
		if(StringUtils.isNotBlank(calenderFlags.getRemainders()) ) {
			JSONObject remainders = new JSONObject();
			JSONArray overrides = new JSONArray();
			for(String methodType : calenderFlags.getRemainders().split(",")) {
				JSONObject type = new JSONObject();
				type.put("method", methodType.split(Const.HASHTAG)[0]);
				type.put("minutes", methodType.split(Const.HASHTAG)[1]);
				overrides.put(type);
			}
			remainders.put("overrides", overrides);
			event.put("reminders", remainders);
		}
		event.put("eventType", calenderFlags.getVisibility());
		if(calenderFlags.isOnlineMeeting()) {
			if(!calenderFlags.isExternalOrg()) {
				JSONObject conferenceData = new JSONObject();
				conferenceData.put("conferenceId", RandomStringUtils.random(10, true, true));
				JSONObject createRequest = new JSONObject();
				JSONObject conferenceSolutionKey = new JSONObject();
				conferenceSolutionKey.put("type", "hangoutsMeet");
				createRequest.put("conferenceSolutionKey", conferenceSolutionKey);
				createRequest.put("requestId", RandomStringUtils.random(10, true, true));
				conferenceData.put("createRequest", createRequest);
				event.put("conferenceData", conferenceData);
			}else {
				event.put("hangoutLink", calenderFlags.getOnlineMeetingUrl().split(Const.HASHTAG)[1]);
				JSONObject conferenceData = new JSONObject();
				conferenceData.put("conferenceId", calenderFlags.getOnlineMeetingUrl().split(Const.HASHTAG)[0]);
				JSONObject conferenceSolutionKey = new JSONObject();
				JSONObject key = new JSONObject();
				conferenceSolutionKey.put("type", calenderFlags.getOnlineMeetingUrl().split(Const.HASHTAG)[2]);
				key.put("key", conferenceSolutionKey);
				conferenceData.put("conferenceSolution", key);
				JSONArray entryPoints = new JSONArray();
				JSONObject video = new JSONObject();
				video.put("entryPointType", "video");
				video.put("uri", calenderFlags.getOnlineMeetingUrl().split(Const.HASHTAG)[1]);
				entryPoints.put(video);
				conferenceData.put("entryPoints", entryPoints);
				event.put("conferenceData", conferenceData);
			}
		}

		if(calenderFlags.getRange()!=null) {
			JSONArray recurrence = new JSONArray();
			//For the recurence rule creation check https://www.rfc-editor.org/rfc/rfc2445 
			String range = createRecurrence(calenderFlags);
			recurrence.put(range);
			event.put("recurrence", recurrence);
		}
		// add the file after uploading to the drive and add here with the file id
		if(attachMents!=null && !attachMents.isEmpty()) {
			JSONArray array = new JSONArray();
			for(AttachmentsData data :attachMents) {
				JSONObject attach = new JSONObject();
				attach.put("fileId", data.getId());
				attach.put("mimeType", data.getContentType());
				attach.put("title", data.getName());
				attach.put("fileUrl", data.getOdataType());
				array.put(attach);
			}
			event.put("attachments", array);
		}
		return event.toString();
	}


	/**
	 * Setting the MailBox Rules from the Filter
	 */

	public EMailRules createEmailRules(Filter mailValue) {
		try {
			EMailRules eMailRules = new EMailRules();
			List<String> _lables = new ArrayList<>();
			if(mailValue.getAction()!=null) {
				if(null!=mailValue.getAction().getAddLabelIds() && !mailValue.getAction().getAddLabelIds().isEmpty()) {
					for(String lables : mailValue.getAction().getAddLabelIds()) {
						if(lables.equalsIgnoreCase(MAIL_FOLDERS.IMPORTANT.name())) {
							eMailRules.setMarkImportance(true);
						}else if(lables.equalsIgnoreCase(MAIL_FOLDERS.STARRED.name())) {
							eMailRules.setFlagged(true);
						}else if(lables.equalsIgnoreCase(MAIL_FOLDERS.TRASH.name())) {
							eMailRules.setDelete(true);
						}else {
							if(lables.equalsIgnoreCase(MAIL_FOLDERS.CATEGORY_PERSONAL.name())){
								_lables.add(MAIL_FOLDERS.INBOX.name());
							}else {
								_lables.add(lables);
							}
						}
					}
				}
				eMailRules.setMailFolder(_lables);
				if( null!=mailValue.getAction().getRemoveLabelIds() && !mailValue.getAction().getRemoveLabelIds().isEmpty()) {
					List<String> rlds = new ArrayList<>();
					for(String lables : mailValue.getAction().getRemoveLabelIds()) {
						if(lables.equalsIgnoreCase(MAIL_FOLDERS.UNREAD.name())) {
							eMailRules.setMarkAsRead(true);
						}else {
							rlds.add(lables);
						}
					}
					eMailRules.setRemoveLables(rlds);
				}
				if(mailValue.getAction().getForward()!=null) {
					eMailRules.setForwards(Arrays.asList(mailValue.getAction().getForward().split(",")));
				}
			}
			if(null!=mailValue.getCriteria()) {
				if(null!=mailValue.getCriteria().getQuery()) {
					eMailRules.setQuery(mailValue.getCriteria().getQuery());
				}
				if(null!=mailValue.getCriteria().getNegatedQuery()) {
					eMailRules.setNegatedQuery(mailValue.getCriteria().getNegatedQuery());
					eMailRules.setNegotiation(true);
				}
				if(null!=mailValue.getCriteria().getFrom()) {
					eMailRules.setFromAddresses(Arrays.asList(mailValue.getCriteria().getFrom().split(",")));
				}
				if(null!=mailValue.getCriteria().getTo()) {
					eMailRules.setSentToAddresses(Arrays.asList(mailValue.getCriteria().getTo().split(",")));
				}
				eMailRules.setAttachments(mailValue.getCriteria().isHasAttachment());

				if(null!=mailValue.getCriteria().getSizeComparison() && mailValue.getCriteria().getSizeComparison().equals("larger")) {
					eMailRules.setMaximumSize(mailValue.getCriteria().getSize());
				}else if(mailValue.getCriteria().getSize()>0) {
					eMailRules.setMinimumSize(mailValue.getCriteria().getSize());
				}
				if(null!=mailValue.getCriteria().getSubject()) {
					eMailRules.setSubjectContains(mailValue.getCriteria().getSubject());
				}
				eMailRules.setEnabled(true);
				eMailRules.setSourceId(mailValue.getId());
			}
			return eMailRules;
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	public EmailFlagsInfo createMailFolderInfo(Label label) {
		EmailFlagsInfo info = new EmailFlagsInfo(); 
		if(label.getName().split("/").length>1) {
			String parent = label.getName().replace(label.getName().substring(label.getName().lastIndexOf("/"),label.getName().length()), "");
			String name = label.getName().replace(parent+"/", "");
			label.setName(name);
			info.setSubFolder(true);
			if(parent.endsWith("/")) {
				info.setParentFolderId(parent.split("/").length>1?parent.split("/")[1]:parent);
			}else {
				info.setParentFolderId(parent.split("/").length>1?parent.split("/")[parent.split("/").length-1]:parent);
			}
		}else {
			info.setParentFolderId("/");
		}
		info.setName(label.getName());
		info.setId(label.getId());
		info.setFolder(label.getName());
		info.setMailFolder(true);
		return info;
	}

	public EmailFlagsInfo createFlagsFromMails(MailValue mailValue,String folder,boolean stopCalendarNotifications,Clouds cloud,Map<String,String>members) {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		if(mailValue.getLabelIds()!=null && !mailValue.getLabelIds().isEmpty()){
			if(!mailValue.getLabelIds().contains(MAIL_FOLDERS.UNREAD.name())) {
				emailFlagsInfo.setRead(true);
			}
			if(mailValue.getLabelIds().contains(MAIL_FOLDERS.IMPORTANT.name())){
				emailFlagsInfo.setImportance("high");
			}
			if(mailValue.getLabelIds().contains(MAIL_FOLDERS.STARRED.name())){
				emailFlagsInfo.setFlagged(true);
			}
			if(mailValue.getLabelIds().contains(MAIL_FOLDERS.DRAFT.name())) {
				emailFlagsInfo.setDraft(true);
			}
		}
		if(mailValue.getPayload().getHeaders()!=null) {
			for(Header header : mailValue.getPayload().getHeaders()) {
				try {
					if(stopCalendarNotifications) {

						// added for removing the calendar events if the calendars is selected
						if("Sender".equals(header.getName())) {
							String _email = ConnectorUtility.splitEmailFromHeader(header.getValue());
							if(_email!=null && _email.equalsIgnoreCase("calendar-notification@google.com")) {
								return null;
							}
						}
						if("Thread-Topic".equals(header.getName()) && header.getValue().contains("event")) {
							return null;
						}
					}

					if(header.getName().equals("From")) {
						emailFlagsInfo.setFrom(ConnectorUtility.splitEmailFromHeader(header.getValue()));
						emailFlagsInfo.setFromName(ConnectorUtility.splitNameFromHeader(header.getValue()));
					}else if(header.getName().equals("To") || header.getName().equals("TO")) {
						String tos = header.getValue();
						if(tos.contains(",")) {
							emailFlagsInfo.setTo(checkAttendees(cloud, members, emailFlagsInfo, tos));
						}else {
							emailFlagsInfo.setTo(Arrays.asList(ConnectorUtility.splitEmailFromHeader(header.getValue())));
						}
					}else if(header.getName().equals("Subject")) {
						emailFlagsInfo.setSubject(header.getValue());
					}else if(header.getName().equals("Cc") || header.getName().equals("CC")) {
						String tos = header.getValue();
						if(tos.contains(",")) {
							emailFlagsInfo.setCc(checkAttendees(cloud, members, emailFlagsInfo, tos));
						}else {
							emailFlagsInfo.setCc(Arrays.asList(ConnectorUtility.splitEmailFromHeader(header.getValue())));
						}
					}else if(header.getName().equals("Bcc") ||header.getName().equals("BCC")) {
						String tos = header.getValue();
						if(tos.contains(",")) {
							emailFlagsInfo.setBcc(checkAttendees(cloud, members, emailFlagsInfo, tos));
						}else {
							emailFlagsInfo.setBcc(Arrays.asList(ConnectorUtility.splitEmailFromHeader(header.getValue())));
						}
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
			emailFlagsInfo.setBodyPreview(mailValue.getSnippet());
			emailFlagsInfo.setHtmlContent(true);
			boolean pickExtFrom = true;
			if(emailFlagsInfo.isGCombo()) {
				pickExtFrom = false;
			}

			if(!pickExtFrom && members.containsKey(emailFlagsInfo.getFrom()) && !emailFlagsInfo.getFrom().equalsIgnoreCase(cloud.getEmail())) {
				return null;
			}
			if(mailValue.getLabelIds().contains(folder)) {
				emailFlagsInfo.setFolder(folder);
			}else {
				mailValue.getLabelIds().forEach(label->{
					if(MappingUtils.isCustomFolder(label)) {
						emailFlagsInfo.setFolder(label);
					}
				});
			}
			if(!members.containsKey(emailFlagsInfo.getFrom()) &&cloud.getDomains()!=null && cloud.getDomains().contains(emailFlagsInfo.getFrom().split(Const.ATTHERATE)[1])) {
				emailFlagsInfo.setGroupMail(true);
			}
			emailFlagsInfo.setCreatedTime(TimeUtils.convertLongToTime(mailValue.getInternalDate(),cloud.getTimeZone()));
			emailFlagsInfo.setSentTime(emailFlagsInfo.getCreatedTime());
			emailFlagsInfo.setHtmlMessage(ConnectorUtility.decodeToString(mailValue.getPayload().getBody().getData()));
			emailFlagsInfo.setSizeInBytes(mailValue.getPayload().getBody().getSize());
			emailFlagsInfo.setParentFolderId(folder);
			emailFlagsInfo.setThreadId(mailValue.getThreadId());
			emailFlagsInfo.setId(mailValue.getId());
			emailFlagsInfo.setTimeZone(cloud.getTimeZone());
			List<AttachmentsData>data = new ArrayList<>();
			if(mailValue.getPayload().getParts()!=null){
				for(Part part : mailValue.getPayload().getParts()) {
					getHtmlFromParts(emailFlagsInfo, data, part);
				}
				if(!data.isEmpty()) {
					emailFlagsInfo.setAttachments(data);
				}
			}
		}
		return emailFlagsInfo;

	}


	private List<String> checkAttendees(Clouds cloud, Map<String, String> members, EmailFlagsInfo emailFlagsInfo, String tos) {
		List<String>tosList = new ArrayList<>();
		if(tos.split(">").length>1) {
			for(String to : tos.split(">")) {
				String _to = ConnectorUtility.splitEmailFromHeader(to);
				if(_to.startsWith(",")) {
					_to= _to.substring(1, _to.length()).trim();
				}
				if(null!=emailFlagsInfo.getFrom() && null!=members&&!members.containsKey(emailFlagsInfo.getFrom()) && cloud.getDomains()!=null && cloud.getDomains().contains(emailFlagsInfo.getFrom().split(Const.ATTHERATE)[1])) {
					_to=_to+":"+true;
				}else {
					_to=_to+":"+false;
				}
				tosList.add(_to.trim());
			}
		}else {
			for(String to : tos.split(",")) {
				String _to = ConnectorUtility.splitEmailFromHeader(to);
				if(null!=emailFlagsInfo.getFrom() && null!=members&&!members.containsKey(emailFlagsInfo.getFrom()) && cloud.getDomains()!=null && cloud.getDomains().contains(emailFlagsInfo.getFrom().split(Const.ATTHERATE)[1])) {
					_to=_to+":"+true;
				}else {
					_to=_to+":"+false;
				}
				tosList.add(_to.trim());
			}
		}
		return tosList;
	}

	private void getHtmlFromParts(EmailFlagsInfo emailFlagsInfo, List<AttachmentsData> data, Part part) {
		if(part.getMimeType()!=null && part.getMimeType().equalsIgnoreCase("text/html")) {
			emailFlagsInfo.setHtmlMessage(ConnectorUtility.decodeToString(part.getBody().getData()));
			List<String>dups = new ArrayList<>();
			if(false && (emailFlagsInfo.getHtmlMessage().contains("a href=\"https://drive.google.com/file") ||emailFlagsInfo.getHtmlMessage().contains("a href=\"https://docs.google.com"))) {
				Map<String,String> links = HttpUtils.getAnchorTags(emailFlagsInfo.getHtmlMessage());
				Iterator<Entry<String, String>> itr = links.entrySet().iterator();
				while(itr.hasNext()) {
					Entry<String,String> str = itr.next();
					if(dups.contains(str.getKey().replace(":", "-"))) {
						continue;
					}
					if(str.getKey().contains("https://drive.google.com/file") || str.getKey().contains("https://docs.google.com")) {
						AttachmentsData attachmentsData = new AttachmentsData();
						attachmentsData.setId(str.getKey().replace(":", "-"));
						dups.add(attachmentsData.getId());
						attachmentsData.setContentType(null);
						attachmentsData.setName(str.getValue());
						attachmentsData.setName(attachmentsData.getName().replace(":", "-"));
						attachmentsData.setInline(true);
						attachmentsData.setSize(Const.GMAIL_ATTACHMENT_LIMIT);
						attachmentsData.setLargeFile(true);
						if(data!=null && !data.contains(attachmentsData)) {
							data.add(attachmentsData);
						}
					}
				}
			}
			emailFlagsInfo.setSizeInBytes(part.getBody().getSize());
		}else if(part.getFilename()!=null && !part.getFilename().isEmpty() && part.getBody()!=null && part.getBody().getAttachmentId()!=null) {
			emailFlagsInfo.setHadAttachments(true);
			AttachmentsData attachmentsData = new AttachmentsData();
			attachmentsData.setId(part.getBody().getAttachmentId());
			attachmentsData.setContentType(part.getMimeType());
			attachmentsData.setName(part.getFilename());
			attachmentsData.setName(attachmentsData.getName().replace(":", "-"));
			attachmentsData.setSize(part.getBody().getSize());
			data.add(attachmentsData);
		}else if(part.getParts()!=null && !part.getParts().isEmpty()) {
			for(Part parts : part.getParts()) {
				getHtmlFromParts(emailFlagsInfo, data, parts);
			}
		}
	}

	public Map<String,String>getMemberDetails(String userId,String adminCloudId){
		MemberDetails memberDetails = cloudsRepoImpl.findMemberDetails(userId, adminCloudId);
		Map<String, String> resultMap = new HashMap<>();
		if(memberDetails!=null) {
			for(String member : memberDetails.getMembers()) {
				if(!resultMap.containsKey(member.split(Const.HASHTAG)[0])) {
					resultMap.put(member.split(Const.HASHTAG)[0], member.split(Const.HASHTAG)[1]);
				}
			}
		}
		return resultMap;
	}

	public UserGroups convertGroupToGroupEmailDetails(Group group,List<String>members) {
		UserGroups groupEmailDetails = new UserGroups();
		groupEmailDetails.setName(group.getName());
		groupEmailDetails.setEmail(group.getEmail());
		groupEmailDetails.setId(group.getId());
		groupEmailDetails.setDescription(group.getDescription());
		groupEmailDetails.setMembers(members);
		groupEmailDetails.setMembersCount(group.getDirectMembersCount());
		return groupEmailDetails;
	}


	public Contacts getContactsInfo( Connection eachConnection) {
		Contacts contacts = new Contacts();
		contacts.setFirstName(eachConnection.getNames().get(0).getGivenName());
		contacts.setLastName(eachConnection.getNames().get(0).getFamilyName());

		if(eachConnection.getEmailAddresses()!=null && !eachConnection.getEmailAddresses().isEmpty()){
			List<Emails> emailAddressList = new ArrayList<>();
			for (EmailAddress eachEmail : eachConnection.getEmailAddresses()) {
				Emails emails = new Emails();
				emails.setEmailAddress(eachEmail.getValue());
				emails.setName(eachConnection.getNames().get(0).getDisplayName());
				emails.setEmailType(eachEmail.getType());
				emailAddressList.add(emails);
			}
			contacts.setEmailAddresses(emailAddressList);
		}
		if(eachConnection.getPhoneNumbers()!=null && !eachConnection.getPhoneNumbers().isEmpty()){
			List<PhoneNumbers> phoneNumberList = new ArrayList<>();
			for (PhoneNumber eachPhoneNo : eachConnection.getPhoneNumbers()) {
				PhoneNumbers numbers = new PhoneNumbers();
				numbers.setPhoneType(eachPhoneNo.getType());
				numbers.setPhoneNo(eachPhoneNo.getCanonicalForm());
				phoneNumberList.add(numbers);
			}
			contacts.setPhoneNumbers(phoneNumberList);

		}
		if(eachConnection.getOrganizations()!=null && !eachConnection.getOrganizations().isEmpty()){
			for (Organization eachOrganization : eachConnection.getOrganizations()) {
				contacts.setCompanyName(eachOrganization.getName());
				contacts.setJobTitle(eachOrganization.getTitle());
				contacts.setDepartment(eachOrganization.getDepartment());
			}

		}

		if(eachConnection.getAddresses()!=null && !eachConnection.getAddresses().isEmpty()){
			List<com.cloudfuze.mail.contacts.entities.Address> addressList = new ArrayList<>();
			for (com.cloudfuze.mail.connectors.google.data.Address eachAddress : eachConnection.getAddresses()) {
				com.cloudfuze.mail.contacts.entities.Address address = new com.cloudfuze.mail.contacts.entities.Address();
				String extendedAddress = eachAddress.getExtendedAddress();
				if(extendedAddress == null){
					address.setStreet(eachAddress.getStreetAddress());
				}else {
					address.setStreet(eachAddress.getStreetAddress()+extendedAddress);
				}
				address.setCountryOrRegion(eachAddress.getCountry());
				address.setCity(eachAddress.getCity());
				address.setState(eachAddress.getRegion());
				address.setPostalCode(eachAddress.getPostalCode());
				if(eachAddress.getType()!=null){
					address.setAddressType(eachAddress.getType());
				}
				addressList.add(address);
			}
			contacts.setAddress(addressList);
		}

		if(eachConnection.getBiographies()!=null && !eachConnection.getBiographies().isEmpty()){
			for (Biography eachBiography : eachConnection.getBiographies()) {
				contacts.setNotes(eachBiography.getValue());
			}
		}
		return contacts;
	}

	public JSONObject createBodyForContacts(String cloudId, Contacts eachContact) {
		JSONObject contactsInput = new JSONObject();
		JSONArray namesArr = new JSONArray();
		//adding first name
		JSONObject name = new JSONObject();
		if(eachContact.getFirstName()!=null){
			name.put("givenName", eachContact.getFirstName());
		}

		//adding last name
		if(eachContact.getLastName()!=null){
			name.put("familyName", eachContact.getLastName());
		}
		namesArr.put(name);
		contactsInput.put("names", namesArr);

		//adding email address
		if(eachContact.getEmailAddresses()!=null && !eachContact.getEmailAddresses().isEmpty()){
			JSONArray emailAddresses = new JSONArray();
			for (Emails eachEmail : eachContact.getEmailAddresses()) {
				JSONObject email = new JSONObject();
				email.put("value", eachEmail.getEmailAddress());
				emailAddresses.put(email);
			}
			contactsInput.put("emailAddresses", emailAddresses);
		}
		//adding phone numbers
		if(eachContact.getPhoneNumbers()!=null && !eachContact.getPhoneNumbers().isEmpty()){
			JSONArray phoneArr = new JSONArray();
			for (PhoneNumbers eachPhoneNo : eachContact.getPhoneNumbers()) {
				try {
					if(eachPhoneNo.getPhoneType()!= null && eachPhoneNo.getPhoneType().equalsIgnoreCase("home")){
						JSONObject homePhone = new JSONObject();
						homePhone.put("value",eachPhoneNo.getPhoneNo());
						homePhone.put("type","home");
						phoneArr.put(homePhone);
					}

					if(eachPhoneNo.getPhoneType()!= null && eachPhoneNo.getPhoneType().equalsIgnoreCase("work")){
						JSONObject workPhone = new JSONObject();
						workPhone.put("value",eachPhoneNo.getPhoneNo());
						workPhone.put("type","work");
						phoneArr.put(workPhone);
					}

					if(eachPhoneNo.getPhoneType()!= null && eachPhoneNo.getPhoneType().equalsIgnoreCase("mobile")){
						JSONObject mobilePhone = new JSONObject();
						mobilePhone.put("value",eachPhoneNo.getPhoneNo());
						mobilePhone.put("type","mobile");
						phoneArr.put(mobilePhone);
					}

					if(eachPhoneNo.getPhoneType() == null){ 
						JSONObject mobilePhone = new JSONObject();
						mobilePhone.put("value",eachPhoneNo.getPhoneNo());
						phoneArr.put(mobilePhone);
					}
				}catch (Exception e) {
					log.warn("cloudId : "+cloudId+" PHONE NO ITERATION ERROR "+ExceptionUtils.getStackTrace(e));	
				}
			}
			contactsInput.put("phoneNumbers", phoneArr);
		}

		//adding address
		if(eachContact.getAddress()!=null && !eachContact.getAddress().isEmpty()){
			JSONArray addressArr = new JSONArray();
			CountryCodes countryCodes = CountryCodes.getInstance();
			for (Address eachAddress : eachContact.getAddress()) {
				if(eachAddress.getAddressType()!=null && eachAddress.getAddressType().equalsIgnoreCase("work")){
					JSONObject businessAddress = new JSONObject();
					businessAddress.put("streetAddress", eachAddress.getStreet());
					businessAddress.put("city", eachAddress.getCity());
					businessAddress.put("region", eachAddress.getState());
					if(eachAddress.getCountryOrRegion()!=null){
						if(eachAddress.getCountryOrRegion().length()<=2){
							businessAddress.put("countryCode", eachAddress.getCountryOrRegion());
						}else {
							String code = countryCodes.getCode(eachAddress.getCountryOrRegion());
							if(code==null){
								businessAddress.put("countryCode", eachAddress.getCountryOrRegion());
							}else {
								businessAddress.put("countryCode", code);
							}
						}
					}
					businessAddress.put("postalCode", eachAddress.getPostalCode());
					businessAddress.put("type", "work");
					addressArr.put(businessAddress);
				}
				if(eachAddress.getAddressType()!=null && eachAddress.getAddressType().equalsIgnoreCase("home")){
					JSONObject homeAddress = new JSONObject();
					homeAddress.put("streetAddress", eachAddress.getStreet());
					homeAddress.put("city", eachAddress.getCity());
					homeAddress.put("region", eachAddress.getState());
					if(eachAddress.getCountryOrRegion()!=null){
						if(eachAddress.getCountryOrRegion().length()<=2){
							homeAddress.put("countryCode", eachAddress.getCountryOrRegion());
						}else {
							String code = countryCodes.getCode(eachAddress.getCountryOrRegion());
							if(code==null){
								homeAddress.put("countryCode", eachAddress.getCountryOrRegion());
							}else {
								homeAddress.put("countryCode", code);
							}
						}
					}
					homeAddress.put("postalCode", eachAddress.getPostalCode());
					homeAddress.put("type", "home");
					addressArr.put(homeAddress);
				}
				if(eachAddress.getAddressType()!=null && eachAddress.getAddressType().equalsIgnoreCase("other")){
					JSONObject otherAddress = new JSONObject();
					otherAddress.put("streetAddress", eachAddress.getStreet());
					otherAddress.put("city", eachAddress.getCity());
					otherAddress.put("region", eachAddress.getState());
					if(eachAddress.getCountryOrRegion()!=null){
						if(eachAddress.getCountryOrRegion().length()<=2){
							otherAddress.put("countryCode", eachAddress.getCountryOrRegion());
						}else {
							String code = countryCodes.getCode(eachAddress.getCountryOrRegion());
							if(code==null){
								otherAddress.put("countryCode", eachAddress.getCountryOrRegion());
							}else {
								otherAddress.put("countryCode", code);
							}
						}
					}
					otherAddress.put("postalCode", eachAddress.getPostalCode());
					otherAddress.put("type", "other");
					addressArr.put(otherAddress);
				} 

				if(eachAddress.getAddressType()== null ){
					JSONObject otherAddress = new JSONObject();
					otherAddress.put("streetAddress", eachAddress.getStreet());
					otherAddress.put("city", eachAddress.getCity());
					otherAddress.put("region", eachAddress.getState());
					if(eachAddress.getCountryOrRegion()!=null){
						if(eachAddress.getCountryOrRegion().length()<=2){
							otherAddress.put("countryCode", eachAddress.getCountryOrRegion());
						}else {
							String code = countryCodes.getCode(eachAddress.getCountryOrRegion());
							if(code==null){
								otherAddress.put("countryCode", eachAddress.getCountryOrRegion());
							}else {
								otherAddress.put("countryCode", code);
							}
						}
					}
					otherAddress.put("postalCode", eachAddress.getPostalCode());
					addressArr.put(otherAddress);
				}
			}
			contactsInput.put("addresses", addressArr);
		}

		//adding personal notes
		if(eachContact.getNotes()!=null){
			JSONArray biographiesArr = new JSONArray();
			JSONObject notes = new JSONObject();
			notes.put("value", eachContact.getNotes());
			notes.put("contentType", "TEXT_PLAIN");
			contactsInput.put("biographies", biographiesArr);
		}
		JSONArray organizationsArr = new JSONArray();
		//adding job title
		JSONObject organizations = new JSONObject();
		if(eachContact.getJobTitle()!=null){
			organizations.put("title", eachContact.getJobTitle());
		}

		//adding company name
		if(eachContact.getCompanyName()!=null){
			organizations.put("name", eachContact.getCompanyName());
		}

		//adding department name
		if(eachContact.getDepartment()!=null){
			organizations.put("department", eachContact.getDepartment());
		}

		organizationsArr.put(organizations);
		return contactsInput;
	}


}
