package com.testing.mail.connectors.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.testing.mail.connectors.MailConnectors;
import com.testing.mail.connectors.impl.helper.OutlookHelper;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.connectors.microsoft.data.BatchRequests;
import com.testing.mail.connectors.microsoft.data.CalendarPermissions;
import com.testing.mail.connectors.microsoft.data.CalenderValue;
import com.testing.mail.connectors.microsoft.data.CalenderViewValue;
import com.testing.mail.connectors.microsoft.data.CalenderViews;
import com.testing.mail.connectors.microsoft.data.Calenders;
import com.testing.mail.connectors.microsoft.data.ContactsList;
import com.testing.mail.connectors.microsoft.data.DomainsList;
import com.testing.mail.connectors.microsoft.data.EmailAttachMentValue;
import com.testing.mail.connectors.microsoft.data.EmailAttachMents;
import com.testing.mail.connectors.microsoft.data.EmailFolders;
import com.testing.mail.connectors.microsoft.data.EmailFoldersValue;
import com.testing.mail.connectors.microsoft.data.EmailList;
import com.testing.mail.connectors.microsoft.data.GroupMembers;
import com.testing.mail.connectors.microsoft.data.GroupValue;
import com.testing.mail.connectors.microsoft.data.GroupsList;
import com.testing.mail.connectors.microsoft.data.MailBoxSettings;
import com.testing.mail.connectors.microsoft.data.MailRules;
import com.testing.mail.connectors.microsoft.data.MailValue;
import com.testing.mail.connectors.microsoft.data.MemberList;
import com.testing.mail.connectors.microsoft.data.MemberValueVO;
import com.testing.mail.connectors.microsoft.data.RefreshTokenResult;
import com.testing.mail.connectors.microsoft.data.Response;
import com.testing.mail.connectors.microsoft.data.Value;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.contacts.dao.ContactsFlagInfo;
import com.testing.mail.contacts.entities.Contacts;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.EmailUserSettings;
import com.testing.mail.dao.entities.RateLimitConfigurer;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.exceptions.MailCreationException;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;
import com.testing.mail.utils.ConnectUtils;
import com.testing.mail.utils.ConvertionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**Connector For The OutLook MailMigration
 * <b>OUTLOOK MAIL </b>
 * <p>
 * Parent Interface <b> MailConnectors</b>
 * </p>
 * @see MailConnectors
 */
@Slf4j
@Service
public class OutLookMailConnector implements MailConnectors {	

	private static final String MAILBOX_NOT_ENABLED_FOR_RESTAPI = "MailboxNotEnabledForRESTAPI";
	private final String GET_ATTACHMENTS_MAIL = "users/%s/messages/%s/attachments"; 
	private final String GET_EVENT_ATTACHMENTS = "users/%s/events/%s/attachments"; 
	private final String GET_EMAIL_CHAINS = "users/%s/messages/%s?$filter=conversationId eq '%s'"; 
	private final String GET_EMAIL_FOLDERS = "users/%s/mailFolders?top=20";
	private final String GET_CHILDREN_EMAIL_FOLDERS = "users/%s/mailFolders/%s/childFolders?top=20";
	private final String GET_DELTA_CHANGES = "users/%s/mailFolders/%s/messages?$filter=receivedDateTime+gt+%s&$select=%s";
	private final String GET_CALENDAR_DELTA_CHANGES ="users/%s/calendars/%s/events?$filter=lastModifiedDateTime+gt+%s";
	private final String baseURL = "https://graph.microsoft.com/v1.0/";
	private final String CREATE_FILE_URL_ADMIN = "users/%s/drive/items/%s/:/%s:/upload.createSession";
	private final String betaURL = "https://graph.microsoft.com/beta/";
	private final String CREATE_MAIL_FOLDER = "/users/%s/mailFolders";
	private final String USERS_DELTA = "/users/delta";
	private final String GET_EMAIL_FOLDER ="users/%s/mailFolders/%s";
	private final String USERS ="/users";
	private final String DEFAULT_MAILBOX = "inbox";
	private final String USER_DRIVE_DETAIL = "users/%s/drive/root";
	private final String SEND_MAIL =baseURL+USERS+"/%s/mailFolders/%s/messages";
	private final String ADD_ATTACHMENT =baseURL+USERS+"/%s/messages";
	private final String DELETE_MAIL =baseURL+USERS+"/%s/messages";
	private final String SEND_MAIL_THREAD =baseURL+USERS+"/%s/messages";
	private final String FSLASH ="/";
	private final String GET_RULES = "users/%s/mailFolders/inbox/messagerules";
	private final String GET_CONTACTS = "users/%s/contacts";
	private final String GET_CALENDERS = "users/%s/calendars/%s";
	private final String GET_MAIL_BOX_SETTINGS = "users/%s/mailBoxSettings";
	private final String GET_CALENDER_EVENTS = "users/%s/calendars/%s/events";
	private final String GET_CALENDER_EVENT_INSTANCES = "users/%s/calendars/%s/events/%s/instances?startDateTime=%s&endDateTime=%s";
	private final String SELECT = "id,createdDateTime,receivedDateTime,sentDateTime,subject,bodyPreview,importance,parentFolderId,conversationId,isRead,isDraft,body,from,toRecipients,ccRecipients,bccRecipients,replyTo,flag,hasAttachments";
	private final String BATCH_OPERATION ="https://graph.microsoft.com/v1.0/$batch";
	private final String CREATE_SHARED_CALENDAR = "https://graph.microsoft.com/v1.0/%s/calendar/calendarPermissions";
	private final String BASE_GROUP = "https://graph.microsoft.com/v1.0/groups";
	private final String ADD_MEMBER_GROUP = "https://graph.microsoft.com/v1.0/groups/%s/members/$ref";
	private final String ADD_OWNER_GROUP = "https://graph.microsoft.com/v1.0/groups/%s/owners/$ref";
	private final String GET_MEMBERS_GROUP = "https://graph.microsoft.com/v1.0/groups/%s/members";
	private final String GET_CALENDERS_EVENTS = "users/%s/calendars/%s/events?$select=*&$filter=start/dateTime ge '%s' OR end/DateTime ge '%s' &$orderby=start/dateTime ASC";

	@Autowired
	RestTemplate restTemplate;
	@Autowired
	AppMongoOpsManager appMongoOpsManager;
	@Autowired 
	private VendorOAuthCredentialImpl vendorOAuthCredentialRepo;
	@Autowired
	private CloudsRepoImpl cloudsRepoImpl;
	private ObjectMapper objMapper = new ObjectMapper();
	private Gson gson = new Gson() ;
	@Autowired
	OutlookHelper outlookHelper;



	@PostConstruct
	public final RateLimitConfigurer getConfigurer() {
		return appMongoOpsManager.findRateLimitConfig(CLOUD_NAME.OUTLOOK);
	}

	/**
	 * <p>
	 *List of Mails Based on MailFolder(Inbox,Sent,Draft)
	 *</p>
	 *<p>
	 *List of Mails Per MailFolder
	 *</p>
	 */
	@Override
	public List<EmailFlagsInfo> getListOfMails(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {

		List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
		try {
			EmailList emailList = null;
			Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
			ConnectUtils.checkClouds(cloud);
			if(cloud.getDriveId()==null) {
				cloud.setDriveId(getDriveDetails(cloud.getId()));
			}
			Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
			String acceeToken = getValidAccessToken(admin.getCredential());
			String memberId=cloud.getMemberId();

			if(StringUtils.isBlank(acceeToken)) { 
				throw new MailMigrationException(ExceptionConstants.ACCESS_TOKEN_NOT_AVAILABLE);
			} 
			String mailFolder = DEFAULT_MAILBOX;
			if(emailFlagsInfo.getFolder()!=null) {
				mailFolder = emailFlagsInfo.getFolder().replace(" ","").trim();
			}
			String url = String.format(baseURL+"users/%s/mailFolders/%s/messages", memberId,mailFolder); 
			url = url+"?top=500&$select="+SELECT;
			log.info("===Total no of mails fetched=="+emailFlagsInfos.size()+"== for the emailId==="+cloud.getEmail());
			if(emailFlagsInfo.getNextPageToken()!=null) { 
				url = emailFlagsInfo.getNextPageToken(); 
			}
			/**
			 * added loop for checking the mailFolders based on Id's if name not not found
			 * like Secondary mailFolders
			 */
			for(int i=0;i<2;i++) {
				try {
					String result = ConnectUtils.getResponse(url, acceeToken, admin.getCredential().getId(),null, CLOUD_NAME.OUTLOOK,cloud.getId());
					if(result!=null) {
						emailList = gson.fromJson(result, EmailList.class);
						emailFlagsInfo.setNextPageToken(emailList.getOdataNextLink()==null?emailList.getOdataDeltaLink():emailList.getOdataNextLink());
						break;
					}
				} catch (HttpClientErrorException e) {
					log.error(ExceptionUtils.getStackTrace(e));
					if(e.getMessage().contains(ExceptionConstants.MAILBOXNOT_FOUND)) {
						throw e; 
					}else if(e.getMessage().contains(ExceptionConstants.MALFORMED_ID)) {
						url = String.format(baseURL+"users/%s/mailFolders/%s/messages", memberId,emailFlagsInfo.getId());
					}
				} 
			}
			List<String> ids = new ArrayList<>();
			final String _mailFolder = mailFolder;
			if (emailList != null && !emailList.getValue().isEmpty()) {
				emailList.getValue().stream()
				.filter(value -> !ids.contains(value.getId()))//for filtering duplicate values
				.peek(value -> ids.add(value.getId())) // for adding the ids
				.map(value -> createFlags(value, _mailFolder)) // mapping those to createFlags
				.filter(Objects::nonNull) // for filtering the non null
				.forEach(emailFlagsInfos::add);
			}
		} catch (Exception e) { 
			throw new MailMigrationException(e); 
		}
		return emailFlagsInfos; 

	}

	/**
	 *For Getting the Root MailFolders like(Inbox,Sent,Drafts)
	 *@param EmailFlagsInfo - for getting the values of MailFolder values
	 *@return List - List of MailFolders returning as emailFlagsInfo
	 */
	@Override
	public List<EmailFlagsInfo> getListOfMailFolders(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		if(cloud.getDriveId()==null) {
			cloud.setDriveId(getDriveDetails(cloud.getId()));
		}
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();
		EmailFolders emailFolders = null;
		String url = String.format(baseURL+ GET_EMAIL_FOLDERS,memberId)+"&$expand=childFolders"; 
		do { 
			if(emailFolders!=null && emailFolders.getOdataNextLink()!=null) {
				url = emailFolders.getOdataNextLink(); 
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId()); 
			if(result!=null) {
				emailFolders = gson.fromJson(result, EmailFolders.class);
			}
			if(emailFolders!=null && !emailFolders.getValue().isEmpty()) {
				for(EmailFoldersValue value : emailFolders.getValue()) {
					try {
						if(value.getDisplayName().equals("Conversation History") || value.getDisplayName().equals("Outbox") || value.getDisplayName().equals("Archive")) {
							continue;
						}
						if(StringUtils.isNotBlank(emailFlagsInfo.getFolder()) && value.getDisplayName().equals(emailFlagsInfo.getFolder())) {
							EmailFlagsInfo flagsInfo = getMailFolderFromValue(value,false);
							emailFlagsInfos.add(flagsInfo); 
							return emailFlagsInfos;
						}
						if(!value.getDisplayName().equals("Conversation History") && StringUtils.isBlank(emailFlagsInfo.getFolder())){
							EmailFlagsInfo flagsInfo = getMailFolderFromValue(value,false);
							if(value.getChildFolders()!=null && !value.getChildFolders().isEmpty()) {
								value.getChildFolders().stream()
								.map(_value -> getMailFolderFromValue(_value,true)) // mapping those to createFlags
								.filter(Objects::nonNull) // for filtering the non null
								.forEach(emailFlagsInfos::add);
							}
							emailFlagsInfos.add(flagsInfo); 
						}
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}while(emailFolders!=null && emailFolders.getOdataNextLink()!=null); 
		return emailFlagsInfos;
	}


	public List<EmailFlagsInfo> getChildernMailFolders(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		if(cloud.getDriveId()==null) {
			cloud.setDriveId(getDriveDetails(cloud.getId()));
		}
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();
		EmailFolders emailFolders = null;
		String url = String.format(baseURL+ GET_CHILDREN_EMAIL_FOLDERS,memberId,emailFlagsInfo.getId())+"&$expand=childFolders"; 
		do { 
			if(emailFolders!=null && emailFolders.getOdataNextLink()!=null) {
				url = emailFolders.getOdataNextLink(); 
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId()); 
			if(result!=null) {
				emailFolders = gson.fromJson(result, EmailFolders.class);
			}
			if(emailFolders!=null && !emailFolders.getValue().isEmpty()) {
				for(EmailFoldersValue value : emailFolders.getValue()) {
					try {
						if(emailFlagsInfo.getFolder()!=null && value.getChildFolderCount()>0) {
							emailFlagsInfo.setId(value.getId());
							List<EmailFlagsInfo>  infos = getChildernMailFolders(emailFlagsInfo);
							if(!infos.isEmpty()) {
								return infos;
							}
						}
						if(StringUtils.isNotBlank(emailFlagsInfo.getFolder()) && value.getDisplayName().equals(emailFlagsInfo.getFolder())) {
							EmailFlagsInfo flagsInfo = getMailFolderFromValue(value,true);
							emailFlagsInfos.add(flagsInfo); 
							return emailFlagsInfos;
						}
						if(!value.getDisplayName().equals("Conversation History") && StringUtils.isBlank(emailFlagsInfo.getFolder())){
							EmailFlagsInfo flagsInfo = getMailFolderFromValue(value,true);
							emailFlagsInfos.add(flagsInfo); 
						}
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}while(emailFolders!=null && emailFolders.getOdataNextLink()!=null); 
		return emailFlagsInfos;
	}

	private EmailFlagsInfo getMailFolderFromValue(EmailFoldersValue value,boolean childFolder) {
		EmailFlagsInfo flagsInfo = new EmailFlagsInfo(); 
		flagsInfo.setId(value.getId());
		flagsInfo.setParentFolderId(value.getParentFolderId());
		flagsInfo.setName(value.getDisplayName());
		flagsInfo.setFolder(value.getDisplayName());
		flagsInfo.setTotalCount(value.getTotalItemCount());
		flagsInfo.setUnreadCount(value.getUnreadItemCount());
		flagsInfo.setSubFolder(childFolder);
		flagsInfo.setSizeInBytes(value.getSizeInBytes());
		flagsInfo.setMailFolder(true);
		return flagsInfo;
	}
	@Override
	public EmailFlagsInfo getMailById(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		return null;
	}


	/**
	 *Creating a secondary MailFolders like : Custom mailFolders
	 *@param - EmailFlagsInfo - values required for creating mailFolders in EmailFlagsInfo
	 *@return  EmailInfo Created Folder EmailInfo
	 *@see EmailInfo
	 */

	@Override
	public EmailInfo createAMailFolder(EmailFlagsInfo emailFlagsInfo) throws MailCreationException {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		EmailInfo emailInfo = null;
		EmailFoldersValue emailFoldersValue =null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminCloudId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		if(emailFlagsInfo.getId()!=null &&null!=emailFlagsInfo.getParentFolderId() && !emailFlagsInfo.getId().equals(emailFlagsInfo.getParentFolderId())) {
			emailFlagsInfo.setId(emailFlagsInfo.getParentFolderId());
		}
		String mailFolder = emailFlagsInfo.getFolder();
		String url = String .format(baseURL+CREATE_MAIL_FOLDER, memberId);
		if(emailFlagsInfo.isSubFolder()) {
			url = url+"/"+emailFlagsInfo.getId()+"/childFolders";
		}
		JSONObject mailFolders = new JSONObject();
		mailFolders.put("displayName", mailFolder);
		mailFolders.put("isHidden", false);
		String result =	ConnectUtils.postResponse(url, acceeToken, mailFolders.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotBlank(result)) {
			emailFoldersValue  = gson.fromJson(result, EmailFoldersValue.class);
		}
		if(!ObjectUtils.isEmpty(emailFoldersValue)) {
			emailInfo = new EmailInfo();
			emailInfo.setId(emailFoldersValue.getId());
			emailInfo.setDestParent(emailFoldersValue.getParentFolderId());
			emailInfo.setMailFolder(emailFoldersValue.getDisplayName());
			emailInfo.setTotalCount(emailFoldersValue.getTotalItemCount());
			emailInfo.setTotalSizeInBytes(emailFoldersValue.getSizeInBytes());
			emailInfo.setDestId(emailFoldersValue.getId());
		}
		return emailInfo;
	}

	/**
	 *Sending Email to the Outlook based on Graph Documentation <br></br>
	 *Refer the Documentation for : <a href="https://learn.microsoft.com/en-us/graph/api/resources/message?view=graph-rest-1.0">SendEmail</a>
	 * need to optimise as for sending an email with time stamp using 4 calls 20-4-2023 
	 * @param - EmailFlagsInfo - values required for creating Email in EmailFlagsInfo
	 * @return EmailInfo Created mail in EmailInfo
	 * @see EmailInfo
	 */
	@Override
	public EmailInfo sendEmail(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Value sentMail = null;
		EmailInfo emailInfo = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String convIndex = null;
		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		String mailFolder = emailFlagsInfo.getFolder().replace(" ", "");
		if(StringUtils.isBlank(mailFolder)) {
			mailFolder = DEFAULT_MAILBOX;
		}
		boolean exceptionOccured = false;		
		String url =null;
		String originalFrom = emailFlagsInfo.getFrom();
		boolean draft = false;
		String convId = emailFlagsInfo.getThreadId();
		String parentId = null;
		String createdId = null;
		boolean isAdmin = false;
		String fromEmail = cloud.getEmail();
		boolean fromExists = false;
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(originalFrom)) {
			fromEmail = originalFrom;
			fromExists = true;
		}
		outlookHelper.validateUser(emailFlagsInfo, mappedEmailDetails);
		if(mailFolder.equalsIgnoreCase("sentitems") || mailFolder.equalsIgnoreCase("drafts") || emailFlagsInfo.isDraft() || emailFlagsInfo.getDestId()!=null) {
			emailFlagsInfo.setFrom(cloud.getEmail());
			//#For Drafts we are not changing any values but for the other we are changing values as admin and those mails will be deleted by EmailPurger.class From admin 
			if(mailFolder.equalsIgnoreCase("drafts")) {
				draft = true;
			} /*
				 * else if(emailFlagsInfo.getDestId()==null){
				 * emailFlagsInfo.setTo(Arrays.asList(admin.getEmail())); }else {
				 * emailFlagsInfo.setFrom(null);
				 * //emailFlagsInfo.setTo(Collections.emptyList()); }
				 */
			url = String .format(SEND_MAIL, fromEmail,mailFolder);
			if(emailFlagsInfo.getTo().isEmpty()) {
				emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
			}
		}else {
			if(!fromExists) {
				fromEmail = admin.getEmail();
			}
			emailFlagsInfo.setFrom(fromEmail);
			isAdmin = true;
			url = String .format(SEND_MAIL, fromEmail,mailFolder);
			if(emailFlagsInfo.getTo().isEmpty()) {
				emailFlagsInfo.setTo(Arrays.asList(cloud.getEmail()));
			}
		}
		if(emailFlagsInfo.isThread()) {
			url = String .format(SEND_MAIL_THREAD, cloud.getEmail())+"/"+emailFlagsInfo.getThreadId()+"/createReply";
			isAdmin = false;
		}
		String input = emailFlagsInfo.isThread()?createBodyForMailThread(emailFlagsInfo, draft).toString() : createBodyForMail(emailFlagsInfo,draft).toString();
		log.info("====Sending an email From OUTLOOK ===="+emailFlagsInfo.getFrom()+"===="+url);
		emailFlagsInfo.setFrom(originalFrom);
		String result = null;
		try {
			if(emailFlagsInfo.getDestId()!=null) {
				emailInfo = new EmailInfo();
				// dest id exists so doing the same data migration with the new content and everything
				log.info("===Destination id exists so with the data we are updating the same ==="+emailFlagsInfo.getDestId()+"-==--"+emailFlagsInfo.getFrom());
				url = url+"/"+emailFlagsInfo.getDestId();
				result = ConnectUtils.patchResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,isAdmin?admin.getId():cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					sentMail  = gson.fromJson(result, Value.class);
					convId = sentMail.getConversationId();
					parentId = sentMail.getParentFolderId();
					createdId = sentMail.getId();
					convIndex = sentMail.getInternetMessageId();
				}
			}else {
				result =	ConnectUtils.postResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,isAdmin?admin.getId():cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					sentMail  = gson.fromJson(result, Value.class);
					convId = sentMail.getConversationId();
					parentId = sentMail.getParentFolderId();
					createdId = sentMail.getId();
					convIndex = sentMail.getInternetMessageId();
				}
				if(sentMail!=null && !emailFlagsInfo.isDraft()) {
					emailInfo = new EmailInfo();
					// for threads changing the to's by using patch call again so that it won't be displayed in user's account or redirected to user's account
					if(emailFlagsInfo.isThread()) {
						emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
						input = createBodyForTimeStampThread(emailFlagsInfo);
						String threadUrl = String .format(SEND_MAIL, isAdmin?admin.getEmail():cloud.getEmail(),mailFolder)+"/"+sentMail.getId();
						result = ConnectUtils.patchResponse(threadUrl, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,isAdmin?admin.getId():cloud.getId());
						sentMail  = gson.fromJson(result, Value.class);
					}
					String _url = String .format(SEND_MAIL, isAdmin?admin.getEmail():cloud.getEmail(),mailFolder)+"/"+sentMail.getId()+"/send";
					result = ConnectUtils.postResponse(_url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,isAdmin?admin.getId():cloud.getId());
					try {
						emailInfo.setMailFolder(mailFolder);
						emailFlagsInfo.setId(sentMail.getConversationId());
						emailInfo.setThreadId(sentMail.getConversationId());
						emailInfo.setDestThreadId(sentMail.getConversationId());
						//adding the conversationId so eventhough it didnt't update here we can update later with this id
						emailInfo.setConvIndex(sentMail.getInternetMessageId());
					} catch (Exception e) {
						exceptionOccured  = true;
						log.error(ExceptionUtils.getStackTrace(e));
						throw e;
					}
					//getting the message as after send the message id will be modified  to find that we are using conversation id so delaying required 
				}
				if(sentMail!=null) {
					emailInfo = new EmailInfo();
					emailInfo.setMailFolder(mailFolder);
					emailFlagsInfo.setId(convId);
					emailInfo.setThreadId(convId);
					emailInfo.setDestThreadId(convId);
					emailInfo.setConvIndex(convIndex);
				}
			}
		} catch (Exception e) {
			exceptionOccured  = true;
			throw new MailMigrationException(e);
		}finally {
			if(sentMail!=null && emailInfo!=null) {
				emailInfo.setId(createdId);
				emailInfo.setThreadId(convId);
				emailInfo.setDestParent(parentId);	
				emailInfo.setConvIndex(convIndex);
			}
			if(exceptionOccured) {
				url = String .format(SEND_MAIL, emailFlagsInfo.isThread()?cloud.getMemberId():admin.getMemberId(),mailFolder);
				if(sentMail!=null) {
					url = url+"/"+sentMail.getId();
					ConnectUtils.deleteResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,emailFlagsInfo.isThread()?cloud.getId():admin.getId());
					emailInfo =null;
				}
			}
		}
		return emailInfo;

	}

	/**
	 *For updating the sender EmailId and sent,Received time in the outlook after sending
	 *<pre>
	 *Ex : admin@testing.com to noReplay@testing.com
	 *</pre>
	 *@param EmailFlagsInfo - for updating the metadata for Email
	 *@return EmailInfo - return the updated Email
	 *@see EmailInfo
	 */	
	@Override
	public  EmailInfo updateMetadata(EmailFlagsInfo emailFlagsInfo) throws Exception {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Value sentMail = null;
		EmailInfo emailInfo = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String url;
		String input;
		String mailFolder = emailFlagsInfo.getFolder();
		String result;
		try {
		try {
			sentMail = getSingleMailByConversationId(emailFlagsInfo);
			} catch (Exception e) {
				throw e;
			}
			if(sentMail==null) {
				throw new Exception("Metadata Not Found");
			}
			url = String .format(SEND_MAIL, cloud.getMemberId(),mailFolder);
			url = url+"/"+sentMail.getId();
			input = createBodyForTimeStamp(emailFlagsInfo,cloud.getEmail());
			result = ConnectUtils.patchResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isNotBlank(result)) {
				sentMail  = gson.fromJson(result, Value.class);
			}
			if(StringUtils.isNoneBlank(result)) {
				emailInfo = new EmailInfo();
				log.info("===SucessFully  Updated the metadata========"+url+"--"+cloud.getId());
				if(mailFolder.equalsIgnoreCase("sentitems") || mailFolder.equalsIgnoreCase("drafts")) {
					emailInfo.setMailFolder(mailFolder);
				}else {
					emailInfo.setMailFolder(DEFAULT_MAILBOX);
				}
				emailInfo.setId(sentMail.getId());
				emailInfo.setDestParent(sentMail.getParentFolderId());
				if(sentMail!=null && emailFlagsInfo.getFrom().equals(sentMail.getFrom().getEmailAddress().getAddress())) {
					emailInfo.setUpdatedMetadata("METADATA UPDATED");
				}else {
					emailInfo.setUpdatedMetadata("NOT UPDATED");
				}
			}else {
				log.info("===Metadata not updated as we dindt found the email in destination with conv id========"+emailFlagsInfo.getId()+"===="+url+"--"+cloud.getId());
			}
		}catch(Exception e) {
			throw e;
		}
		return emailInfo;
	}

	private String getValidAccessToken(VendorOAuthCredential credential) {
		return outlookHelper.getValidAccessToken(credential);
	}


	public String refreshTokenWithRefreshToken(String id) {
		VendorOAuthCredential credential = vendorOAuthCredentialRepo.findById(id);
		OAuthKey keys = appMongoOpsManager.findOAuthKeyByCloud(CLOUD_NAME.OUTLOOK);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "refresh_token");
		form.add("client_id", keys.getClientId());
		form.add("scope", "https://graph.microsoft.com/.default");
		form.add("client_secret", keys.getClientSecret());
		form.add("refresh_token", credential.getRefreshToken());

		String url = "https://login.windows.net/common/oauth2/token";
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			String result = response.getBody();
			RefreshTokenResult tokenResult = objMapper.readValue(result, RefreshTokenResult.class);
			if(tokenResult.getError()== null) {
				VendorOAuthCredential newCredential = convertToVendorCredential(tokenResult);
				newCredential.setId(id);
				newCredential.setLastRefreshed(LocalDateTime.now());
				vendorOAuthCredentialRepo.save(newCredential);
				return newCredential.getAccessToken();	
			} else {
				log.error(Thread.currentThread().getName() + " :Error refreshing access token, "+ tokenResult.toString());
			}
		} catch (RestClientException e) {
			log.info("Request Failed while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			log.info("Error while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}



	public String refreshToken(String id) {
		VendorOAuthCredential credential = vendorOAuthCredentialRepo.findById(id);
		OAuthKey keys = appMongoOpsManager.findOAuthKeyByCloud(CLOUD_NAME.OUTLOOK);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", keys.getClientId());
		form.add("scope", "https://graph.microsoft.com/.default");
		form.add("client_secret", keys.getClientSecret());
		//form.add("refresh_token", credential.getRefreshToken());

		String url = "https://login.microsoftonline.com/";
		String domain = credential.getId().split(":")[0].split("@")[1];
		url = url+domain+"/oauth2/v2.0/token";
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			String result = response.getBody();
			RefreshTokenResult tokenResult = objMapper.readValue(result, RefreshTokenResult.class);
			if(tokenResult.getError()== null) {
				VendorOAuthCredential newCredential = convertToVendorCredential(tokenResult);
				newCredential.setId(id);
				newCredential.setLastRefreshed(LocalDateTime.now());
				vendorOAuthCredentialRepo.save(newCredential);
				return newCredential.getAccessToken();	
			} else {
				log.error(Thread.currentThread().getName() + " :Error refreshing access token, "+ tokenResult.toString());
			}
		} catch (RestClientException e) {
			log.info("Request Failed while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			log.info("Error while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}




	public VendorOAuthCredential convertToVendorCredential(RefreshTokenResult tokenResult) {
		return outlookHelper.convertToVendorCredential(tokenResult);
	}


	public String createDelegateToken(String id,boolean isGovernmentCloud) throws Exception{
		VendorOAuthCredential credential = vendorOAuthCredentialRepo.findById(id);
		OAuthKey keys = appMongoOpsManager.findOAuthKeyByCloud(CLOUD_NAME.OUTLOOK);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String baseUrl = "https://login.microsoftonline.com/";
		if(isGovernmentCloud){
			baseUrl = "https://login.microsoftonline.us/";
		}
		String url = (baseUrl+"common/oauth2/token");
		MultiValueMap<String, String> formData = new LinkedMultiValueMap();
		formData.add("grant_type", "client_credentials");
		formData.add("client_id", keys.getClientId());
		formData.add("scope", "https://graph.microsoft.com/.default");
		formData.add("client_secret", keys.getClientSecret());
		formData.add("redirect_uri", keys.getRedirectUrl());
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			String result = response.getBody();
			RefreshTokenResult tokenResult = objMapper.readValue(result, RefreshTokenResult.class);
			if(tokenResult.getError()== null) {
				VendorOAuthCredential newCredential = outlookHelper.convertToVendorCredential(tokenResult);
				newCredential.setId(id);
				newCredential.setLastRefreshed(LocalDateTime.now());
				vendorOAuthCredentialRepo.save(newCredential);
				return newCredential.getAccessToken();	
			} else {
				log.error(Thread.currentThread().getName() + " :Error refreshing access token, "+ tokenResult.toString());
			}
		} catch (RestClientException e) {
			log.info("Request Failed while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			log.info("Error while refreshing token  : " + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}	

	public VendorOAuthCredential getAccessTokenByAuthorizationCode(String code) {
		OAuthKey keys = appMongoOpsManager.findOAuthKeyByCloud(CLOUD_NAME.OUTLOOK);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", keys.getClientId());
		form.add("client_secret", keys.getClientSecret());
		form.add("redirect_uri", keys.getRedirectUrl());
		form.add("code", code);
		String url ="https://login.microsoftonline.com/common/oauth2/v2.0/token";
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form,headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity,String.class);
			String result = response.getBody();
			if(result == null) {
				log.error("Response body is null While getting accesstoken from code");
				return null;
			}
			log.error(result);
			RefreshTokenResult tokenResult = objMapper.readValue(result, RefreshTokenResult.class);
			log.info(tokenResult.getScope());
			if(StringUtils.isBlank(tokenResult.getAccessToken())){
				return null;
			}
			return outlookHelper.convertToVendorCredential(tokenResult);
		} catch(RestClientException e){
			log.info("Request Failed while refreshing token  : "+ExceptionUtils.getStackTrace(e));
		} catch(Exception e){
			log.info("Error while refreshing token  : "+ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/**
	 *Getting the calendar Attachments From the cloud based on Message/Event Id
	 *@param EmailFlagsInfo - getAttachments based on the values from the EmailFlagsInfo
	 *@return List - List of Attachments
	 *@see AttachmentsData
	 */

	@Override
	public List<AttachmentsData> getAttachments(EmailFlagsInfo emailFlagsInfo){

		EmailAttachMents emailAttachments = null;
		List<AttachmentsData>attachments = new ArrayList<>(); 
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		} 
		do {
			String url = String.format(baseURL+ GET_ATTACHMENTS_MAIL,memberId,emailFlagsInfo.getId());
			if(emailFlagsInfo.isEvents()) {
				url = String.format(baseURL+GET_EVENT_ATTACHMENTS,memberId,emailFlagsInfo.getId());
			}
			if(emailAttachments!=null && emailAttachments.getOdataNextLink()!=null) {
				url = emailAttachments.getOdataNextLink(); 
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(result!=null) {
				try {
					emailAttachments = gson.fromJson(result, EmailAttachMents.class);
				}
				catch (Exception e) {
					log.info(ExceptionUtils.getStackTrace(e)); 
				}
			}
			boolean largeFile = false;
			if(emailAttachments!=null && !emailAttachments.getValue().isEmpty()) {
				for(EmailAttachMentValue value : emailAttachments.getValue()) { 
					if(emailFlagsInfo.isLargeFile() && value.getSize()<Const.ATTACHMENT_LIMIT) {
						continue;
					}

					try {
						AttachmentsData data = new AttachmentsData();
						data.setContentBytes(value.getContentBytes());
						data.setContentType(value.getContentType()); 
						if(value.getContentBytes()==null || value.getContentType()==null) {
							url = url+"/"+value.getId()+"/$value";
							result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
							if(StringUtils.isNoneBlank(result)) {
								data.setContentBytes(result);
							}
						}
						data.setOdataType(value.getOdataType());
						data.setName(value.getName());
						data.setSize(value.getSize());
						data.setParentMessageId(emailFlagsInfo.getId());
						data.setParentFolderId(emailFlagsInfo.getParentFolderId());
						data.setEncoded(true); 
						if(value.getSize()>3*1024*1024) {
							largeFile = true;
						}
						data.setLargeFile(largeFile);
						attachments.add(data); 
					} catch (Exception e) {
						log.info(ExceptionUtils.getStackTrace(e)); 
					} 
				}
			} 
		}while(emailAttachments!=null && emailAttachments.getOdataNextLink()!=null); 
		return attachments; 
	}

	public List<EmailFlagsInfo> getListOfMailsPerChain(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException{

		List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>(); 
		try {
			EmailList emailList = null;
			Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
			ConnectUtils.checkClouds(cloud);
			Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
			if(admin.getCredential()==null) {
				admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
			}
			String acceeToken = getValidAccessToken(admin.getCredential());
			String memberId=cloud.getMemberId();


			if(StringUtils.isBlank(acceeToken)) {
				return Collections.emptyList(); 
			} 
			String mailFolder = DEFAULT_MAILBOX; 
			if(emailFlagsInfo.getLabels()!=null && !emailFlagsInfo.getLabels().isEmpty()) { 
				mailFolder = emailFlagsInfo.getLabels().get(0);
			}

			do {
				String url = String.format(baseURL+ GET_EMAIL_CHAINS,memberId,mailFolder,emailFlagsInfo.getThreadId());
				if(emailList!=null && emailList.getOdataNextLink()!=null) {
					url = emailList.getOdataNextLink(); 
				}
				String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(result!=null) { 
					try {
						emailList = gson.fromJson(result, EmailList.class);
					} catch (Exception e) {
						log.info(ExceptionUtils.getStackTrace(e));
						if(e.getMessage().contains(MAILBOX_NOT_ENABLED_FOR_RESTAPI)) {
							throw e; 
						} 
					}
				}
				if(emailList!=null && !emailList.getValue().isEmpty()) {
					for(Value value : emailList.getValue()) {
						EmailFlagsInfo info = createFlags(value, mailFolder);
						if(info!=null) {
							emailFlagsInfos.add(info); 
						}
					} 
				}
			}while(emailList!=null && emailList.getOdataNextLink()!=null); 
		} catch (Exception e) { throw new MailMigrationException(e);
		} 
		return emailFlagsInfos; 
	}


	public EmailFlagsInfo createFlags(Value value,String userId) {
		if(value==null) {
			return null; 
		} 
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		if(value.getRemoved()!=null) {
			emailFlagsInfo.setDeleted(true);
			emailFlagsInfo.setId(value.getId());
			return emailFlagsInfo;
		}
		try {
			if(value.getFrom()!=null) {
				emailFlagsInfo.setFrom(value.getFrom().getEmailAddress().getAddress());
				emailFlagsInfo.setFromName(value.getFrom().getEmailAddress().getName());
			}
			if(value.getToRecipients()!=null && !value.getToRecipients().isEmpty()) {
				List<String> to = value.getToRecipients().stream()
						.map(toRecipents -> toRecipents.getEmailAddress().getAddress() == null
						? toRecipents.getEmailAddress().getName()
								: toRecipents.getEmailAddress().getAddress())
						.collect(Collectors.toList());

				emailFlagsInfo.setTo(to);
			}

			if(value.getCcRecipients()!=null && !value.getCcRecipients().isEmpty()) {
				List<String> to = value.getCcRecipients().stream()
						.map(toRecipents -> toRecipents.getEmailAddress().getAddress() == null
						? toRecipents.getEmailAddress().getName()
								: toRecipents.getEmailAddress().getAddress())
						.collect(Collectors.toList());
				emailFlagsInfo.setCc(to);

			}
			if(value.getBccRecipients()!=null && !value.getBccRecipients().isEmpty()) {
				List<String> to = value.getBccRecipients().stream()
						.map(toRecipents -> toRecipents.getEmailAddress().getAddress() == null
						? toRecipents.getEmailAddress().getName()
								: toRecipents.getEmailAddress().getAddress())
						.collect(Collectors.toList());
				emailFlagsInfo.setBcc(to);

			}
			if(value.getReplyTo()!=null && !value.getReplyTo().isEmpty()) {
				List<String> to = value.getReplyTo().stream()
						.map(toRecipents -> toRecipents.getEmailAddress().getAddress() == null
						? toRecipents.getEmailAddress().getName()
								: toRecipents.getEmailAddress().getAddress())
						.collect(Collectors.toList());
				emailFlagsInfo.setReplyTo(to);

			} 
			if(value.getFlag()!=null && value.getFlag().getFlagStatus().equals("notFlagged")) {
				emailFlagsInfo.setFlagged(false);
			}else {
				emailFlagsInfo.setFlagged(true);
			}
			emailFlagsInfo.setMessage(value.getBodyPreview());
			emailFlagsInfo.setSubject(value.getSubject());
			if(value.getBody()!=null && value.getBody().getContentType().equals("html")) {
				emailFlagsInfo.setHtmlContent(true);
				emailFlagsInfo.setHtmlMessage(value.getBody().getContent());
			}else if(value.getBody()!=null) {
				emailFlagsInfo.setHtmlMessage(value.getBody().getContent());
			}
			emailFlagsInfo.setBodyPreview(value.getBodyPreview());
			emailFlagsInfo.setDraft(value.getIsDraft());
			emailFlagsInfo.setParentFolderId(value.getParentFolderId());
			emailFlagsInfo.setId(value.getId());
			emailFlagsInfo.setThreadId(value.getConversationId());
			emailFlagsInfo.setLabels(Arrays.asList(userId));
			emailFlagsInfo.setFolder(userId);
			emailFlagsInfo.setSentTime(value.getSentDateTime());
			emailFlagsInfo.setCreatedTime(value.getCreatedDateTime());
			emailFlagsInfo.setImportance(value.getImportance());
			emailFlagsInfo.setHadAttachments(value.getHasAttachments());
			emailFlagsInfo.setRead(value.getIsRead());
		} catch(Exception e) { 
			log.info(ExceptionUtils.getStackTrace(e)); 
		} 
		return emailFlagsInfo; 
	}



	public EmailFlagsInfo createFlags(CalenderViewValue value,String userId,String adminEmail) {
		return outlookHelper.createFlags(value,  adminEmail);
	}

	/**
	 * Checking email  for getting Admin details<p></p>
	 * Checking Admin status and having the right License 
	 * @param accessToken - accessToken for the admin
	 * @param AdminEmail - adminEmailId to get the details of the user Based on Email(Ex : austin@syncgalaxy.com)
	 */
	public Clouds getAdminDetails(String accessToken,String adminEmail) {
		Clouds admin = null;
		MemberValueVO vo = getResponseForMe(accessToken,adminEmail);
		if(vo!= null) {
			boolean isAdmin = checkUserAdminStatus(accessToken);
			if(isAdmin) {
				admin = getAdminDetails(new ConnectFlags(accessToken,CLOUD_NAME.OUTLOOK.name(),vo.getMail()==null?vo.getUserPrincipalName():vo.getMail(), vo.getId(),vo.getId(),null, null, null));				
			}
		}
		return admin;
	}



	private boolean checkUserAdminStatus(String accessToken) {
		boolean isAdministrator = false;
		String url = betaURL+"users?$select=displayName";
		String response = null;
		try {
			response = ConnectUtils.getResponse(url, accessToken, null, null, CLOUD_NAME.OUTLOOK,null);
		} catch (Exception e) {
			if(e.getMessage().contains("Authentication_RequestFromNonPremiumTenantOrB2CTenant")) {
				url = betaURL+"users?$select=displayName,id";
				response = ConnectUtils.getResponse(url, accessToken,  null, null, CLOUD_NAME.OUTLOOK,null);
			}
		}		
		if(response != null) {
			MemberList userList = gson.fromJson(response, MemberList.class);
			if(userList.getValue() != null) isAdministrator = true;

		}
		return isAdministrator;

	}

	public MemberValueVO getResponseForMe(String token,String adminEmail) {
		String url = adminEmail != null ?baseURL+USERS+FSLASH+adminEmail :baseURL+"me" ;

		String response = ConnectUtils.getResponse(url, token, null,  null, CLOUD_NAME.OUTLOOK,null);		
		if (response != null) {
			try {
				return objMapper.readValue(response, MemberValueVO.class);
			} catch (JsonProcessingException e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public Clouds getAdminDetails(ConnectFlags connectFlags){
		String domain = connectFlags.getEmailId().split(Const.ATTHERATE)[1];
		domain = domain.replace("-", "");
		String url = betaURL+domain+"/users/"+connectFlags.getEmailId()+"?$select=displayName,userPrincipalName,id,proxyAddresses";
		Clouds user = null;
		String result = ConnectUtils.getResponse(url, connectFlags.getAccessToken(), null, null, CLOUD_NAME.OUTLOOK,null);

		if (result != null) {
			MemberValueVO adminUser;
			try {
				adminUser = objMapper.readValue(result, MemberValueVO.class);
				user = new Clouds();
				String email = adminUser.getUserPrincipalName() != null ? adminUser.getUserPrincipalName().toLowerCase() : adminUser.getMail();
				user.setEmail(email);
				user.setName(adminUser.getDisplayName() != null ? adminUser.getDisplayName() : "");
				user.setMemberId(adminUser.getId());
				user.setSmtps(adminUser.getProxyAddresses());
				//user.setMetaUrl(baseURL+domain);
			} catch (JsonProcessingException e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return user;
	}

	@Override
	public List<String> getDomains(ConnectFlags connectFlags) {
		Clouds cloud = cloudsRepoImpl.findAdmin(connectFlags.getAdminMemberId(), connectFlags.getUserId());
		ConnectUtils.checkClouds(cloud);
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		List<String>domains = new ArrayList<>();
		String updatedToken =  getValidAccessToken(cloud.getCredential());
		String response = null;
		String url = baseURL+"domains";
		DomainsList domainsList = null;
		response = ConnectUtils.getResponse(url, updatedToken, null,cloud.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotBlank(response)) {
			domainsList = gson.fromJson(response, DomainsList.class);
		}
		if(ObjectUtils.isNotEmpty(domainsList) && !domainsList.getValue().isEmpty()) {
			domainsList.getValue().forEach(domain->{
				domains.add(domain.getId());
			});
		}
		return domains;
	}

	@Override
	public List<Clouds> getUsersList(ConnectFlags connectFlags) {

		String userListURL = baseURL+connectFlags.getEmailId().split("@")[1]+USERS_DELTA+"?$select=displayName,id,userPrincipalName,mail,proxyAddresses";
		if(connectFlags.getNextPageToken()!=null) {
			userListURL = connectFlags.getNextPageToken();
		}
		List<Clouds> saasUserList = new ArrayList<>();
		MemberList userList = null;
		Clouds cloud = cloudsRepoImpl.findAdmin(connectFlags.getAdminMemberId(), connectFlags.getUserId());
		ConnectUtils.checkClouds(cloud);
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String updatedToken =  getValidAccessToken(cloud.getCredential());
		Clouds saaSUser;
		try {
			String userListString = ConnectUtils.getResponse(userListURL, updatedToken, null,cloud.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId());
			userList = gson.fromJson(userListString, MemberList.class);
			if (userList != null) {
				if(userList!=null && StringUtils.isNotBlank(userList.getOdataDeltaLink())) {
					cloud.setDeltaChangeId(userList.getOdataDeltaLink());
				}
				cloud.setNextPageToken(userList.getOdataNextLink());
				connectFlags.setNextPageToken(userList.getOdataNextLink());
				for(MemberValueVO user:  userList.getValue()) {
					saaSUser = new Clouds();
					if(user.getDisplayName()==null || user.getUserPrincipalName()==null || user.getMail()==null || connectFlags.getEmailId().equals(user.getUserPrincipalName())) {
						continue;
					}

					saaSUser.setName(user.getDisplayName());
					saaSUser.setEmail(user.getMail()==null?user.getUserPrincipalName():user.getMail());

					try {
						EmailInfo mailBox = getInbox(updatedToken,user.getId());
						boolean active = mailBox!=null;
						saaSUser.setActive(active);
					}catch(Exception e) {
						saaSUser.setActive(false);
						saaSUser.setErrorDescription(ExceptionUtils.getStackTrace(e));
					}
					saaSUser.setMemberId(user.getId());
					saaSUser.setSmtps(user.getProxyAddresses());
					saasUserList.add(saaSUser);
				}
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}finally {
			cloudsRepoImpl.save(cloud);
		}

		return saasUserList;
	}

	@Override
	public List<Clouds> getDeltaUsersList(ConnectFlags connectFlags) {
		String userListURL = baseURL+connectFlags.getEmailId().split("@")[1]+USERS_DELTA+"?$select=displayName,id,userPrincipalName,mail,proxyAddresses";
		List<Clouds> saasUserList = new ArrayList<>();
		MemberList userList = null;
		Clouds cloud = cloudsRepoImpl.findAdmin(connectFlags.getAdminMemberId(), connectFlags.getUserId());
		ConnectUtils.checkClouds(cloud);
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String updatedToken =  getValidAccessToken(cloud.getCredential());
		Clouds saaSUser;
		if(cloud.getDeltaChangeId()!=null) {
			userListURL = cloud.getDeltaChangeId();
		}
		do {
			try {
				String userListString = ConnectUtils.getResponse(userListURL, updatedToken, null,cloud.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId());
				userList = gson.fromJson(userListString, MemberList.class);
				if (userList != null) {
					userListURL = userList.getOdataNextLink();

					for(MemberValueVO user:  userList.getValue()) {
						saaSUser = new Clouds();
						if(connectFlags.getEmailId().equals(user.getUserPrincipalName())) {
							continue;
						}
						saaSUser.setName(user.getDisplayName());
						saaSUser.setEmail(user.getMail()==null?user.getUserPrincipalName():user.getMail());
						saaSUser.setMemberId(user.getId());
						if(user.getProxyAddresses()==null || user.getProxyAddresses().isEmpty() || user.getMail()==null) {
							saaSUser.setActive(false);
						}
						saaSUser.setSmtps(user.getProxyAddresses());
						saaSUser.setActive(true);
						saasUserList.add(saaSUser);
					}
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		} while (userList != null && userList.getOdataNextLink() != null && !userList.getValue().isEmpty());
		if(userList!=null && StringUtils.isNotBlank(userList.getOdataDeltaLink())) {
			cloud.setDeltaChangeId(userList.getOdataDeltaLink());
			cloudsRepoImpl.save(cloud);
		}
		return saasUserList;
	}

	@Override
	public String getDeltaChangeId(EmailFlagsInfo connectFlags) {
		if(connectFlags.getFolder()==null) {
			return null;
		}
		Clouds cloud = cloudsRepoImpl.findOne(connectFlags.getCloudId());
		ConnectUtils.checkClouds(cloud);
		if(!cloud.isAdmin()) {
			cloud = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		}
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		connectFlags.setFolder(connectFlags.getFolder().replace(" ", ""));
		String changesList = null;
		if(connectFlags.isEvents()) {
			changesList = betaURL+String.format(GET_CALENDAR_DELTA_CHANGES, cloud.getMemberId(),connectFlags.getFolder()==null?"Calendar":connectFlags.getFolder(),connectFlags.getCreatedTime()+"Z");
			changesList = changesList+"&top=50";
		}else {
			changesList = betaURL+String.format(GET_DELTA_CHANGES, cloud.getMemberId(),connectFlags.getFolder()==null?DEFAULT_MAILBOX:connectFlags.getFolder(),connectFlags.getCreatedTime()+"Z",SELECT);
			changesList = changesList+"&top=50";

		}
		return changesList;
	}

	@Override
	public EmailInfo getLabel(EmailFlagsInfo emailFlagsInfo) {
		EmailInfo emailInfo = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		if(cloud.getDriveId()==null) {
			cloud.setDriveId(getDriveDetails(cloud.getId()));
		}
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		ConnectUtils.checkClouds(admin);
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();
		EmailFoldersValue emailFolders = null;
		String mailFolder = emailFlagsInfo.getFolder();
		try {
			String url = String.format(baseURL+ GET_EMAIL_FOLDER,memberId,mailFolder); 
			String result = ConnectUtils.getResponse(url, acceeToken, null ,admin.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId()); 
			if(result!=null) {
				emailFolders = gson.fromJson(result, EmailFoldersValue.class);
			}
			if(emailFolders!=null && !emailFolders.getIsHidden()) {
				emailInfo = getEmailInfoFromEmailFlags(emailFolders);	
			}
		} catch (Exception e) {
			//For custom labels not fetching based on name so we are fetching based on name in GetListOfMailFolders by Folder as name.
			if(e.getMessage().contains("ErrorInvalidIdMalformed")) {
				List<EmailFlagsInfo> infos = getListOfMailFolders(emailFlagsInfo);
				if(ObjectUtils.isNotEmpty(infos) && !infos.isEmpty()) {
					EmailFlagsInfo info = infos.get(0);
					return getEmailInfoFromEmailFlags(info);
				}
			}else{
				log.info(ExceptionUtils.getStackTrace(e));
			}
		}
		return emailInfo;
	}



	public EmailInfo checkUserMailBoxStatus(String acceessToken,String memberId) {
		EmailInfo emailInfo = null;
		EmailFoldersValue emailFolders = null;
		try {
			String url = String.format(baseURL+ GET_EMAIL_FOLDER,memberId,DEFAULT_MAILBOX); 
			String result = ConnectUtils.getResponse(url, acceessToken, null ,null,  CLOUD_NAME.OUTLOOK,null); 
			if(result!=null) {
				emailFolders = gson.fromJson(result, EmailFoldersValue.class);
			}
			if(emailFolders!=null) {
				emailInfo = getEmailInfoFromEmailFlags(emailFolders);	
			}
		} catch (Exception e) {
			return null;
		}
		return emailInfo;
	}

	private EmailInfo getEmailInfoFromEmailFlags(EmailFoldersValue emailFolders) {
		EmailInfo emailInfo = new EmailInfo();
		emailInfo.setFolder(true);
		emailInfo.setMailFolder(emailFolders.getDisplayName());
		emailInfo.setSourceId(emailFolders.getId());
		emailInfo.setId(emailFolders.getId());
		emailInfo.setSourceParent(emailFolders.getParentFolderId());
		emailInfo.setTotalCount(emailFolders.getTotalItemCount());
		emailInfo.setUnreadCount(emailFolders.getUnreadItemCount());
		emailInfo.setTotalSizeInBytes(emailFolders.getSizeInBytes());
		return emailInfo;
	}

	private EmailInfo getEmailInfoFromEmailFlags(EmailFlagsInfo emailFolders) {
		EmailInfo emailInfo = new EmailInfo();
		emailInfo.setFolder(true);
		emailInfo.setMailFolder(emailFolders.getName());
		emailInfo.setSourceId(emailFolders.getId());
		emailInfo.setId(emailFolders.getId());
		emailInfo.setSourceParent(emailFolders.getParentFolderId());
		emailInfo.setTotalCount(emailFolders.getTotalCount());
		emailInfo.setUnreadCount(emailFolders.getUnreadCount());
		emailInfo.setTotalSizeInBytes(emailFolders.getSizeInBytes());
		return emailInfo;
	}

	public EmailInfo getInbox(String acceeToken,String memberId) {
		EmailInfo emailInfo = null;

		EmailFoldersValue emailFolders = null;
		String mailFolder = DEFAULT_MAILBOX;

		try {
			String url = String.format(baseURL+ GET_EMAIL_FOLDER,memberId,mailFolder); 
			String result = ConnectUtils.getResponse(url, acceeToken, null ,null,  CLOUD_NAME.OUTLOOK,null); 
			if(result!=null) {
				emailFolders = gson.fromJson(result, EmailFoldersValue.class);
			}
			if(emailFolders!=null && !emailFolders.getIsHidden()) {
				emailInfo = getEmailInfoFromEmailFlags(emailFolders);	
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return emailInfo;
	}


	@Override
	public CalenderInfo getCalendar(CalenderFlags emailFlagsInfo) {
		CalenderInfo emailInfo = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		ConnectUtils.checkClouds(admin);
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();
		Calenders emailFolders = null;
		String mailFolder = emailFlagsInfo.getCalendar();
		if(mailFolder==null) {
			mailFolder = "Calendar";
		}
		String url = String.format(baseURL+ GET_CALENDERS,memberId,""); 
		String result = ConnectUtils.getResponse(url, acceeToken, null ,admin.getCredential().getId(),  CLOUD_NAME.OUTLOOK,cloud.getId()); 
		if(result!=null) {
			emailFolders = gson.fromJson(result, Calenders.class);
		}
		if(emailFolders!=null && !emailFolders.getValue().isEmpty()) {
			for(CalenderValue calenderValue : emailFolders.getValue()) {
				if(calenderValue.getName().equals(mailFolder)) {
					try {
						emailInfo = new CalenderInfo();
						emailInfo.setCalender(true);
						emailInfo.setStartTime(calenderValue.getName());
						emailInfo.setSourceId(calenderValue.getId());
						emailInfo.setId(calenderValue.getId());
						emailInfo.setSourceParent("/");
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}
		return emailInfo;
	}


	public EmailInfo getMailFolder(ConnectFlags connectFlags,String emailId,String id) {
		EmailInfo emailInfo = null;

		String acceeToken = connectFlags.getAccessToken();
		String memberId=emailId;
		EmailFoldersValue emailFolders = null;
		String url = String.format(baseURL+ GET_EMAIL_FOLDER,memberId,DEFAULT_MAILBOX); 
		String result = ConnectUtils.getResponse(url, acceeToken, null, id,  CLOUD_NAME.OUTLOOK,null); 
		if(result!=null) {
			emailFolders = gson.fromJson(result, EmailFoldersValue.class);
		}
		if(emailFolders!=null && !emailFolders.getIsHidden()) {
			emailInfo = getEmailInfoFromEmailFlags(emailFolders);	
		}
		return emailInfo;
	}

	private JSONObject createBodyForMail(EmailFlagsInfo emailFlagsInfo,boolean draft) throws MailMigrationException {
		return outlookHelper.createBodyForMail(emailFlagsInfo);
	}


	private JSONObject createBodyForMailThread(EmailFlagsInfo emailFlagsInfo,boolean draft) throws MailMigrationException {
		return outlookHelper.createBodyForMailThread(emailFlagsInfo);
	}

	private JSONObject bodyForFileAttachments(AttachmentsData data) {
		return outlookHelper.bodyForFileAttachments(data);
	}


	public Value getSingleMailByConversationId(EmailFlagsInfo emailFlagsInfo) throws Exception {
		return outlookHelper.getSingleMailByConversationId(emailFlagsInfo);
	}
	
	public CalenderViewValue getEventByConversationId(CalenderFlags emailFlagsInfo) throws Exception {
		return outlookHelper.getEventByConversationId(emailFlagsInfo);
	}
	//getEventByConversationId
	
	public Value getSingleMailBySubject(EmailFlagsInfo emailFlagsInfo) throws Exception {
		return outlookHelper.getSingleMailBySubject(emailFlagsInfo);
	}


	private Value getEventMessage(List<Value> values,String eventId,String iCalUid) {
		if(!values.isEmpty()) {
			for(Value value : values) {
				if(eventId.equals(value.getEvent().getId()) || iCalUid.equals(value.getEvent().getICalUId())) {
					return value;
				}
			}
		}
		return null;
	}

	@Override
	public List<EmailFlagsInfo> getDeltaChanges(EmailFlagsInfo emailFlagsInfo,String deltaChangeId) throws MailMigrationException {
		List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
		String deltaLink = null;
		try {
			if(StringUtils.isBlank(deltaChangeId)) {
				throw new MailMigrationException("LatestChangeId is Null");
			}
			if(emailFlagsInfo.isEvents()) {
				return getCalendarDeltaChanges(emailFlagsInfo, deltaChangeId);
			}
			EmailList emailList = null;
			Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
			ConnectUtils.checkClouds(cloud);
			Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
			if(admin.getCredential()==null) {
				admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
			}
			String acceeToken = getValidAccessToken(admin.getCredential());
			if(StringUtils.isBlank(acceeToken)) { 
				return Collections.emptyList(); 
			} 
			String mailFolder = emailFlagsInfo.getFolder();
			String url = deltaChangeId; 

			do { 
				if(emailList!=null && emailList.getOdataNextLink()!=null) { 
					url = emailList.getOdataNextLink();
					deltaLink = emailList.getOdataNextLink();
					if(deltaLink==null) {
						deltaLink = emailList.getOdataDeltaLink();
					}
				}
				String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(result!=null) {
					try {
						emailList = gson.fromJson(result, EmailList.class);
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						if(e.getMessage().contains(MAILBOX_NOT_ENABLED_FOR_RESTAPI)) {
							throw e; 
						} 
					} 
				}
				if(emailList!=null && !emailList.getValue().isEmpty()) { 
					for(Value value : emailList.getValue()) { 
						EmailFlagsInfo info = createFlags(value, mailFolder);
						if(info!=null) {
							emailFlagsInfos.add(info);
						}
					}
				}
				deltaLink = emailList.getOdataNextLink();
				if(deltaLink==null) {
					deltaLink = emailList.getOdataDeltaLink();
				}
			}while(emailList!=null && emailList.getOdataNextLink()!=null); 
		} catch (Exception e) { 
			throw new MailMigrationException(e); 
		}	
		if(StringUtils.isNotBlank(deltaLink)) {
			emailFlagsInfo.setNextPageToken(deltaLink);
		}
		return emailFlagsInfos; 
	}

	private String createBodyForTimeStamp(EmailFlagsInfo emailFlagsInfo,String email) {
		return outlookHelper.createBodyForTimeStamp(emailFlagsInfo, email);
	}
	private String createBodyForTimeStampThread(EmailFlagsInfo emailFlagsInfo) {
		return outlookHelper.createBodyForTimeStampThread(emailFlagsInfo).toString();
	}

	@Override
	public List<EMailRules> getMailBoxRules(EmailFlagsInfo emailFlagsInfo){
		MailRules mailRules = null; 
		List<EMailRules> emailRules = new ArrayList<>();

		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		} 
		String url = String.format(baseURL+GET_RULES, memberId);
		String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			mailRules = gson.fromJson(result, MailRules.class);	
		}

		if(ObjectUtils.isNotEmpty(mailRules) && !mailRules.getValue().isEmpty()) {
			mailRules.getValue().forEach(value->{
				EMailRules rules=  createEmailRules(value);
				if(rules!=null) {
					emailRules.add(rules);
				}
			});
		}
		return emailRules;
	}

	/** Creating MailBox Rule Based on EMailRules
	 * <br></br>
	 * @see EMailRules
	 */
	@Override
	public EMailRules createMailBoxRule(EMailRules eMailRules,EmailFlagsInfo emailFlagsInfo){
		MailValue mailRules = null; 

		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		} 
		String url = String.format(baseURL+GET_RULES, memberId);
		String input = createBodyForRule(eMailRules);
		String result = ConnectUtils.postResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			mailRules = gson.fromJson(result, MailValue.class);	
		}
		if(ObjectUtils.isNotEmpty(mailRules)) {
			return createEmailRules(mailRules);
		}
		return null;
	}

	/**
	 * Moving Mails from one MailFolder to another MailFolder Like Inbox to Archive
	 * or Inbox to DeletedItems
	 */
	@Override
	public EmailInfo moveEmails(EmailFlagsInfo emailFlagsInfo,EmailInfo emailInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		Value movedMail = null;

		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		for(int i=0;i<=2;i++) {
			try {
				String url = String .format(SEND_MAIL, memberId,emailInfo.getMailFolder());
				url = url+"/"+emailInfo.getId()+"/move";
				JSONObject body = new JSONObject();
				body.put("destinationId", emailFlagsInfo.getFolder());
				String result = ConnectUtils.postResponse(url, acceeToken, body.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(!StringUtils.isEmpty(result)) {
					movedMail = gson.fromJson(result, Value.class);
					break;
				}
			}catch(HttpClientErrorException e) {
				if(e.getMessage().contains("404 Not Found")) {
					log.info("==email not found=="+cloud.getId()+"==so retrying with the conv id==retrying on time=="+i);
					try {
						EmailFlagsInfo info = new EmailFlagsInfo();
						info.setId(emailFlagsInfo.getThreadId());
						info.setFolder(emailInfo.getMailFolder());
						info.setFrom(emailInfo.getFromMail());
						info.setCloudId(emailFlagsInfo.getCloudId());
						info.setDeleted(true);
						info.setConvIndex(emailFlagsInfo.getConvIndex());
						Value value = getSingleMailByConversationId(info);
						if(value!=null) {
							emailInfo.setId(value.getId());
						}
					} catch (Exception e1) {
						throw e;
					}
				}else if(e.getMessage().contains("ErrorInvalidIdMalformed")) {
					List<EmailFlagsInfo> list = getListOfMailFolders(emailFlagsInfo);
					if(list!=null && !list.isEmpty()) {
						emailFlagsInfo.setFolder(list.get(0).getId());
					}

				}
			}
		}
		if(ObjectUtils.isNotEmpty(movedMail)) {
			emailInfo.setMailFolder(emailFlagsInfo.getFolder());
			emailInfo.setDestParent(movedMail.getParentFolderId());
			emailInfo.setId(movedMail.getId());
			emailInfo.setThreadId(movedMail.getConversationId());
		}
		return emailInfo;
	}




	private EMailRules createEmailRules(MailValue mailValue) {
		return outlookHelper.createEmailRules(mailValue);
	}


	public String uploadLargeFile(AttachmentsData data,EmailFlagsInfo emailFlagsInfo,boolean event) throws IOException {
		return outlookHelper.uploadLargeFile(data, emailFlagsInfo, event);
	}

	@Override
	public List<CalenderInfo>getCalendars(CalenderFlags emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		Calenders calenders = null;

		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		if(cloud.getTimeZone()==null) {
			cloud.setTimeZone(getMailBoxSettings(cloud.getId()));
			cloudsRepoImpl.save(cloud);
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		String url = String.format(baseURL+GET_CALENDERS, memberId,"");
		String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			calenders = gson.fromJson(result, Calenders.class);
		}
		List<CalenderInfo>calendars = new ArrayList<>();
		calenders.getValue().forEach(calnder->{
			CalenderInfo info= createEmailInfoForCalender(calnder);
			calendars.add(info);
		});
		return calendars;
	}


	public List<CalenderInfo>getCalendarsViewsByTime(CalenderFlags emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		CalenderViews calenders = null;

		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		String url = String.format(baseURL+GET_CALENDER_EVENTS, memberId,emailFlagsInfo.getId());
		url =url+"?$filter=organizer/emailAddress/name eq '"+cloud.getName()+"'";
		if(emailFlagsInfo.getNextPageToken()!=null) {
			url = emailFlagsInfo.getNextPageToken(); 
		}
		String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			calenders = gson.fromJson(result, CalenderViews.class);
		}
		emailFlagsInfo.setNextPageToken(calenders.getOdataNextLink());
		List<CalenderInfo>calendars = new ArrayList<>();
		calenders.getValue().forEach(calnder->{
			if(calnder.getOrganizer().getEmailAddress().getAddress().equals(cloud.getEmail())) {
				CalenderInfo info= createInforViews(calnder,emailFlagsInfo.getId());
				calendars.add(info);
			}
		});
		return calendars;
	}


	@Override
	public List<CalenderInfo>getCalendarEvents(CalenderFlags emailFlagsInfo){
		
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		CalenderViews calenders = null;

		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		String url = String.format(baseURL+GET_CALENDER_EVENTS, memberId,emailFlagsInfo.getId());
		//url =url+"?$filter=organizer/emailAddress/name eq '"+cloud.getName()+"'";
		if(emailFlagsInfo.getNextPageToken()!=null) {
			url = emailFlagsInfo.getNextPageToken(); 
		}
		String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			calenders = gson.fromJson(result, CalenderViews.class);
		}
		emailFlagsInfo.setNextPageToken(calenders.getOdataNextLink());
		List<CalenderInfo>calendars = new ArrayList<>();
		calenders.getValue().forEach(calnder->{
			//if(calnder.getOrganizer().getEmailAddress().getAddress().equals(cloud.getEmail())) {
				CalenderInfo info= createInforViews(calnder,emailFlagsInfo.getId());
				calendars.add(info);
			//}
		});
		return calendars;
	}

	/**For Creating the Calenders using Graph api
	 * @param - CalenderFlags : Containing the required values for fetching the values from Outlook
	 * @return  - CalenderInfo :  
	 */
	@Override
	public CalenderInfo createCalender(CalenderFlags calenderFlags) {
		log.info("==For Creating the calenders here=="+calenderFlags.getCloudId());
		Clouds cloud = cloudsRepoImpl.findOne(calenderFlags.getCloudId());
		CalenderValue calenders = null;

		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			throw new MailMigrationException("Issue with Accesstoken");
		}
		String url = String.format(baseURL+GET_CALENDERS, memberId,"");
		if(calenderFlags.getNextPageToken()!=null) {
			url = calenderFlags.getNextPageToken();
		}
		JSONObject body = new JSONObject();
		body.put("name", calenderFlags.getCalendar());
		String result =	ConnectUtils.postResponse(url, acceeToken, body.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotBlank(result)) {
			calenders = gson.fromJson(result, CalenderValue.class);
		}
		if(calenders!=null) {
			return createEmailInfoForCalender(calenders);
		}
		return null;

	}

	@Override
	public CalenderInfo createCalenderEvent(CalenderFlags calenderFlags) {
		log.info("==For Creating the CalenderEvent =="+calenderFlags.getCloudId());
		Clouds cloud = cloudsRepoImpl.findOne(calenderFlags.getCloudId());
		CalenderViewValue calenders = null;
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getId());
		String dstTimeZone = cloud.getTimeZone();
		String accessToken = getValidAccessToken(admin.getCredential());
		String memberId = cloud.getMemberId();

		if(StringUtils.isBlank(accessToken)) { 
			throw new MailMigrationException(ExceptionConstants.ACCESS_TOKEN_NOT_AVAILABLE);
		}
		try {
			List<String>attendess = calenderFlags.getAttendees();
//			if(calenderFlags.getDestId()==null && !attendess.isEmpty()) {
//				calenderFlags.setAttendees(Arrays.asList(cloud.getEmail()));
//			}
			String calendar = calenderFlags.getCalendar();
			if(calendar==null) {
				calendar = "Calendar";
			}
			if(mappedEmailDetails.containsKey(calenderFlags.getOrganizer())) {
				memberId = mappedEmailDetails.get(calenderFlags.getOrganizer());
			}
			String url = String.format(baseURL+GET_CALENDER_EVENTS, memberId,calendar);
			String input = createBodyForEvent(calenderFlags,dstTimeZone,cloud);
			String result = null;
			if(calenderFlags.getDestId()!=null) {
				url = url+"/"+calenderFlags.getDestId();
				result = ConnectUtils.patchResponse(url, accessToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			}else {
				result = ConnectUtils.postResponse(url, accessToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			}

			if(StringUtils.isNotBlank(result)) {
				calenders = gson.fromJson(result, CalenderViewValue.class);
			}
			calenderFlags.setAttendees(attendess);
			if(calenders!=null) {
				return createInforViews(calenders,calenderFlags.getCalendar());
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));	
			throw e;
		}
		return null;

	}

	/**
	 * Body For calendar Event
	 * <p>Check the RecurenceRule clearly it will impact on event Creation in outlook</p>
	 * @return JSONObject.toString()
	 */
	private String createBodyForEvent(CalenderFlags emailFlagsInfo,String dstTimeZone,Clouds clouds) {
		return outlookHelper.createBodyForEvent(emailFlagsInfo, dstTimeZone,clouds);
	}

	private CalenderInfo createInforViews(CalenderViewValue value,String sourceParent) {
		return outlookHelper.createInforViews(value, sourceParent);
	}

	private CalenderInfo createEmailInfoForCalender(CalenderValue value) {
		return outlookHelper.createEmailInfoForCalender(value);
	}


	/**For updating the timestamp and from and names<p></p>
	 *
	 *<p></p>Refer Documentation for any queries: <i><a href = "https://learn.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxprops/f6ab1613-aefe-447d-a49c-18217230b148?redirectedfrom=MSDN">exchange_server_protocols </a></i>
	 */
	private String createBodyForTimeStampEvent(CalenderFlags emailFlagsInfo) {
		return outlookHelper.createBodyForTimeStampEvent(emailFlagsInfo);
	}

	/** Input for creating MailBoxRule  <br></br>
	 * For Reference See Documentation of MS-Graph <a href= "https://learn.microsoft.com/en-us/graph/api/mailfolder-post-messagerules?view=graph-rest-1.0&tabs=http">Rule-Creation</a> 
	 * @param EmailRules -POJO for creating body for a MailBoxRule
	 * @return String - Json to String converted data
	 */
	public String createBodyForRule(EMailRules eMailRules) {
		return outlookHelper.createBodyForRule(eMailRules);
	}


	@Override
	public List<String> addAttachment(EmailFlagsInfo emailFlagsInfo,boolean event) throws IOException {
		return outlookHelper.addAttachment(emailFlagsInfo, event);
	}

	@Override
	public  CalenderInfo updateCalendarMetadata(CalenderFlags emailFlagsInfo) throws Exception {
		if(emailFlagsInfo.isExternalOrg()) {
			return updateAtendeeCalendarMetadata(emailFlagsInfo);
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		CalenderViewValue sentMail = null;
		CalenderInfo emailInfo = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getId());

		String url;
		String input;
		String mailFolder = emailFlagsInfo.getCalendar();
		if(mailFolder==null) {
			mailFolder = "Calendar";
		}
		String memberId =  cloud.getMemberId();
		if(mappedEmailDetails.containsKey(emailFlagsInfo.getOrganizer())) {
			memberId = mappedEmailDetails.get(emailFlagsInfo.getOrganizer());
		}
		String result = null;
		try {
			url = String .format(baseURL+GET_CALENDER_EVENTS, memberId,mailFolder);
			url = url+"/"+emailFlagsInfo.getId();
			log.info("===For Updating the metadata========"+url);
			for(int i=0;i<2;i++) {
				input = createBodyForTimeStampEvent(emailFlagsInfo);
				result = ConnectUtils.patchResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					sentMail  = gson.fromJson(result, CalenderViewValue.class);
				}
				log.info(sentMail.getOrganizer().getEmailAddress().getAddress()+"---"+sentMail.getCreatedDateTime()+"--");
				if(sentMail.getOrganizer().getEmailAddress().getAddress().equals(emailFlagsInfo.getOrganizer())) {
					break;
				}else {
					sentMail = null;
				}
				Thread.sleep(1000);
			}
			if(StringUtils.isNoneBlank(result) && sentMail!=null) {
				emailInfo = new CalenderInfo();
				log.info("===SucessFully  Updated the metadata========"+url);
				emailInfo.setId(sentMail.getId());
				if(StringUtils.isNotBlank(result)) {
					emailInfo.setUpdatedMetadata("METADATA UPDATED");
				}else {
					emailInfo.setUpdatedMetadata("NOT UPDATED");
				}
				if(!sentMail.getOrganizer().getEmailAddress().getAddress().equals(emailFlagsInfo.getOrganizer())) {
					throw new MailMigrationException("Metadata Not Updated");
				}
			}else {
				log.info("===Metadata not updated as we dindt found the email in destination with conv id========"+emailFlagsInfo.getId()+"===="+url);
				throw new MailMigrationException("Metadata Not Updated");
			}
		}catch(Exception e) {
			throw e;
		}
		return emailInfo;
	}
	
	
	
	public  CalenderInfo updateEventInstance(CalenderFlags emailFlagsInfo) throws Exception {
		if(emailFlagsInfo.isExternalOrg()) {
			return updateAtendeeCalendarMetadata(emailFlagsInfo);
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		CalenderViewValue sentMail = null;
		CalenderInfo emailInfo = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getId());

		String url;
		String input;
		String mailFolder = emailFlagsInfo.getCalendar();
		if(mailFolder==null) {
			mailFolder = "Calendar";
		}
		String memberId =  cloud.getMemberId();
		if(mappedEmailDetails.containsKey(emailFlagsInfo.getOrganizer())) {
			memberId = mappedEmailDetails.get(emailFlagsInfo.getOrganizer());
		}
		String result = null;
		try {
			url = String .format(baseURL+GET_CALENDER_EVENTS, memberId,mailFolder);
			url = url+"/"+emailFlagsInfo.getId();
			log.info("===For Updating the EVENT INSTANCE========"+url);
				input = createBodyForEvent(emailFlagsInfo, null,cloud);
				result = ConnectUtils.patchResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					sentMail  = gson.fromJson(result, CalenderViewValue.class);
				}
				log.info(sentMail.getOrganizer().getEmailAddress().getAddress()+"---"+sentMail.getCreatedDateTime()+"--");
			if(StringUtils.isNoneBlank(result) && sentMail!=null) {
				emailInfo = new CalenderInfo();
				log.info("===SucessFully  Updated the EVENT INSTANCE========"+url);
				emailInfo.setId(sentMail.getId());
				if(StringUtils.isNotBlank(result)) {
					emailInfo.setUpdatedMetadata("EVENT INSTANCE UPDATED");
				}else {
					emailInfo.setUpdatedMetadata("EVENT INSTANCE NOT UPDATED");
				}
			}else {
				log.info("===Metadata not updated as we dindt found the email in destination with conv id========"+emailFlagsInfo.getId()+"===="+url);
				throw new MailMigrationException("Metadata Not Updated");
			}
			List<String>declined = new ArrayList<>();
			if(emailFlagsInfo.getAttendees()!=null && !emailFlagsInfo.getAttendees().isEmpty()) {
				for(String tos : emailFlagsInfo.getAttendees()) {
					if(emailFlagsInfo.getOrganizer()!=null && emailFlagsInfo.getOrganizer().equalsIgnoreCase(tos.split(":")[0])) {
						continue;
					}
					if(tos.split(":").length>1) {
						String type = outlookHelper.createResponseType(tos);
						if(type.equals("declined")) {
							declined.add(tos.split(":")[0]);
						}
					}
				}
			}
			if(!declined.isEmpty()) {
				for(String decline : declined) {
					try {
						Clouds _cloud = cloudsRepoImpl.findCloudsByEmailIdUsingAdmin(cloud.getUserId(), decline, cloud.getAdminCloudId());
						if(null == _cloud) {
							continue;
						}
						CalenderFlags calenderFlags = new CalenderFlags();
						calenderFlags.setCloudId(_cloud.getId());
						calenderFlags.setICalUId(emailFlagsInfo.getICalUId());
						calenderFlags.setDestId(sentMail.getSeriesMasterId());
						calenderFlags.setEndTime(sentMail.getOccurrenceId().split("\\.")[2]);
						calenderFlags.setOrganizer(decline);
						declineEvent(calenderFlags);
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}catch(Exception e) {
			throw e;
		}
		return emailInfo;
	}
	
	public  CalenderInfo declineEvent(CalenderFlags emailFlagsInfo) throws Exception {
		if(emailFlagsInfo.isExternalOrg()) {
			return updateAtendeeCalendarMetadata(emailFlagsInfo);
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		CalenderInfo emailInfo = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getId());
		CalenderViewValue calValeu = getEventByConversationId(emailFlagsInfo);
		if(calValeu!=null) {
			emailFlagsInfo.setId(calValeu.getId());
		}else {
			return null;
		}
		String url;
		String input;
		String mailFolder = emailFlagsInfo.getCalendar();
		if(mailFolder==null) {
			mailFolder = "Calendar";
		}
		String memberId =  cloud.getMemberId();
		if(mappedEmailDetails.containsKey(emailFlagsInfo.getOrganizer())) {
			memberId = mappedEmailDetails.get(emailFlagsInfo.getOrganizer());
		}
		try {
			url = String .format(baseURL+GET_CALENDER_EVENTS, memberId,mailFolder);
			url = url+"/"+emailFlagsInfo.getId()+"/decline";
			log.info("===For Updating the EVENT INSTANCE DECLINED========"+url);
				input = outlookHelper.createBodyForEventDecline(emailFlagsInfo, null,cloud);
				 ConnectUtils.postResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			
		}catch(Exception e) {
			throw e;
		}
		return emailInfo;
	}
	
	
	
	public  CalenderInfo updateAtendeeCalendarMetadata(CalenderFlags emailFlagsInfo) throws Exception {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		CalenderViews sentMail = null;
		CalenderInfo emailInfo = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String url;
		String input;
		String mailFolder = "Calendar";
		String result = null;
		CalenderViewValue _sentMail  = null;
		try {
			url = String .format(baseURL+GET_CALENDER_EVENTS, cloud.getMemberId(),mailFolder);
			url = url+"?$filter=iCalUId eq '"+emailFlagsInfo.getICalUId()+"'";
			log.info("===For Updating the metadata========"+url);
			for(int i=0;i<2;i++) {
				if(_sentMail!=null) {
					break;
				}
				input = createBodyForTimeStampEvent(emailFlagsInfo);
				result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					sentMail  = gson.fromJson(result, CalenderViews.class);
				}
				if(sentMail!=null && !sentMail.getValue().isEmpty()) {
					for(CalenderViewValue value : sentMail.getValue()) {
						url = String .format(baseURL+GET_CALENDER_EVENTS, cloud.getMemberId(),mailFolder);
						url = url+"/"+value.getId();
						result = ConnectUtils.patchResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
						if(StringUtils.isNotBlank(result)) {
							_sentMail  = gson.fromJson(result, CalenderViewValue.class);
						}
						log.info(_sentMail.getOrganizer().getEmailAddress().getAddress()+"---"+_sentMail.getCreatedDateTime()+"--");
						if(_sentMail.getOrganizer().getEmailAddress().getAddress().equals(emailFlagsInfo.getOrganizer())) {
							break;
						}else {
							sentMail = null;
						}
						Thread.sleep(1000);
					}
				}
			}
			if(StringUtils.isNoneBlank(result) && _sentMail!=null) {
				emailInfo = new CalenderInfo();
				log.info("===SucessFully  Updated the metadata========"+url);
				emailInfo.setId(_sentMail.getId());
				if(StringUtils.isNotBlank(result)) {
					emailInfo.setUpdatedMetadata("METADATA UPDATED");
				}else {
					emailInfo.setUpdatedMetadata("NOT UPDATED");
				}
				if(!_sentMail.getOrganizer().getEmailAddress().getAddress().equals(emailFlagsInfo.getOrganizer())) {
					throw new MailMigrationException("Metadata Not Updated");
				}
			}else {
				log.info("===Metadata not updated as we dindt found the email in destination with conv id========"+emailFlagsInfo.getId()+"===="+url);
				throw new MailMigrationException("Metadata Not Updated");
			}
		}catch(Exception e) {
			throw e;
		}
		return emailInfo;
	}

	private String createBodyForItemAttachMent(AttachmentsData attachmentsData) {
		JSONObject item = new JSONObject();
		item.put("@odata.type", "microsoft.graph.event");

		item.put("subject", attachmentsData.getContentBytes());
		// need to check for the string to normal conversion to mail so that the data will migrate based on that
		return null;
	}


	@Override
	public boolean deleteEmails(EmailFlagsInfo emailFlagsInfo,boolean event) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Value sentMail = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String url = null;
		String result;
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		String originalFrom = emailFlagsInfo.getFrom();
		boolean fromExists = false;
		String memberId = cloud.getMemberId();
		if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(originalFrom)) {
			fromExists = true;
		}
		if(!fromExists) {
			memberId = admin.getMemberId();
		}else {
			if(emailFlagsInfo.isCopy()) {
				memberId = admin.getMemberId();
			}else {
				memberId = mappedEmailDetails.get(originalFrom);
			}
		}
		String id = emailFlagsInfo.getId(); 
		try {
			if(emailFlagsInfo.getId()==null || emailFlagsInfo.isCopy()) {
				sentMail = getSingleMailByConversationId(emailFlagsInfo);
			}
			if(sentMail!=null) {
				id = sentMail.getId();
			}
			url = String .format(DELETE_MAIL, memberId);
			if(event) {
				url = String.format(baseURL+GET_CALENDER_EVENTS, memberId,"Calendar");
			}
			url = url+"/"+id;
			result = ConnectUtils.deleteResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isEmpty(result)) {
				return true;
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	private List<EmailFlagsInfo> getCalendarDeltaChanges(EmailFlagsInfo emailFlagsInfo,String deltaChangeId){

		List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
		String deltaLink = null;
		try {
			if(StringUtils.isBlank(deltaChangeId)) {
				throw new MailMigrationException("LatestChangeId is Null for delta changes");
			}
			CalenderViews calendarViews = null;
			Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
			ConnectUtils.checkClouds(cloud);
			Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
			if(admin.getCredential()==null) {
				admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
			}
			String acceeToken = getValidAccessToken(admin.getCredential());
			if(StringUtils.isBlank(acceeToken)) { 
				return Collections.emptyList(); 
			} 
			String mailFolder = emailFlagsInfo.getFolder();
			String url = deltaChangeId;

			do { 
				if(calendarViews!=null && calendarViews.getOdataNextLink()!=null) { 
					url = calendarViews.getOdataNextLink();
					deltaLink = calendarViews.getOdataNextLink();
				}
				String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(result!=null) {
					try {
						calendarViews = gson.fromJson(result, CalenderViews.class);
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
						if(e.getMessage().contains(MAILBOX_NOT_ENABLED_FOR_RESTAPI)) {
							throw e; 
						} 
					} 
				}

				if(calendarViews!=null && !calendarViews.getValue().isEmpty()) { 
					calendarViews.getValue().forEach(value->{
						EmailFlagsInfo info = createFlags(value, mailFolder, cloud.getEmail());
						if(info!=null) {
							emailFlagsInfos.add(info);
						}
					});
				}
				deltaLink = calendarViews.getOdataNextLink();
			}while(calendarViews!=null && calendarViews.getOdataNextLink()!=null); 
		} catch (Exception e) { 
			throw new MailMigrationException(e); 
		}	
		if(StringUtils.isNotBlank(deltaLink)) {
			emailFlagsInfo.setNextPageToken(deltaLink);
		}
		return emailFlagsInfos; 

	}

	/**
	 * MailBox settings For timeZone of Cloud and other details
	 * <p>Refer MSGraph API for Documentation</p>
	 * @return User Cloud timeZone
	 * @param cloudId : cloudId
	 */
	public String getMailBoxSettings(String cloudId) {
		MailBoxSettings mailBoxSettings = null;
		Clouds cloud = cloudsRepoImpl.findOne(cloudId);
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		String url = String.format(baseURL+GET_MAIL_BOX_SETTINGS, cloud.getMemberId());
		String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotBlank(result)) {
			mailBoxSettings = gson.fromJson(result, MailBoxSettings.class);
		}
		if(ObjectUtils.isNotEmpty(mailBoxSettings)) {
			return outlookHelper.TIMEZONE_MAPPINGS.get(mailBoxSettings.getTimeZone());
		}
		return null;
	}

	public Object createBatchRequest(EmailFlagsInfo emailFlagsInfo,List<String> requests) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		BatchRequests batchRequests = null;
		JSONObject request = new JSONObject();
		JSONArray batchs = new JSONArray();
		if(requests.isEmpty()) {
			return null;
		}
		Integer value = 1;
		for(String req : requests) {

			JSONObject body = new JSONObject();
			JSONObject headers = new JSONObject();
			headers.put("Content-Type", "application/json");
			String folderUrl = String.format(USERS+"/%s/mailFolders/%s/messages/%s", admin.getMemberId(),emailFlagsInfo.getFolder(),req);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", value);
			jsonObject.put("body", body);
			jsonObject.put("headers", headers);
			jsonObject.put("method", "DELETE");
			jsonObject.put("url", folderUrl);
			batchs.put(jsonObject);
			value = value+1;
		}
		request.put("requests", batchs);
		String result = ConnectUtils.postResponse(BATCH_OPERATION, acceeToken, request.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			batchRequests = gson.fromJson(result, BatchRequests.class);
		}	
		if(ObjectUtils.isNotEmpty(batchRequests)) {
			for(Response response : batchRequests.getResponses()) {
			}
		}
		return null;
	}

	@Override
	public List<EmailUserSettings> getSettings(EmailFlagsInfo emailFlagsInfo) {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	@Override
	public EmailUserSettings createUpdateSettings(EmailUserSettings emailUserSettings, EmailFlagsInfo emailFlagsInfo) {
		// TODO Auto-generated method stub
		return null;
	}



	public AttachmentsData uploadFile(AttachmentsData data,EmailFlagsInfo emailFlagsInfo) throws IOException {
		return outlookHelper.uploadFile(data, emailFlagsInfo);
	}

	/**
	 * Get Drive Details for Uploading the attachments to the OneDrive
	 * Loading only once saving in Clouds so not required for everyTime
	 */
	private String getDriveDetails(String cloudId) {
		return outlookHelper.getDriveDetails(cloudId);
	}


	@Override
	public List<UserGroups> getGroupEmailDetails(String adminCloudId) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		GroupsList groupsList = null;
		List<UserGroups> groups = new ArrayList<>();
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String acceeToken = getValidAccessToken(cloud.getCredential());

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		String url = BASE_GROUP;
		do {
			if(groupsList!=null && groupsList.getOdataNextLink()!=null) {
				url = groupsList.getOdataNextLink();
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null,cloud.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isNotBlank(result)) {
				groupsList = gson.fromJson(result, GroupsList.class);
			}
			if(groupsList!=null && !groupsList.getValue().isEmpty()) {
				groupsList.getValue().forEach(group->
				groups.add(ConvertionUtils.convertGroupToGroupEmailDetails(group, null))
						);
			}
		}while(groupsList!=null && groupsList.getOdataNextLink()!=null);
		return groups;
	}

	@Override
	public List<String> getMembersFromGroup(String adminCloudId, String groupId) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		List<String> listOfMembers = new ArrayList<>();
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String acceeToken = getValidAccessToken(cloud.getCredential());

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		GroupMembers groupValue = null;
		String url = String.format(GET_MEMBERS_GROUP,groupId);
		String result = ConnectUtils.getResponse(url, acceeToken, null,cloud.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotBlank(result)) {
			groupValue = gson.fromJson(result, GroupMembers.class);
			if(groupValue!=null) {
				groupValue.getValue().forEach(value->
					listOfMembers.add(value.getMail()+Const.HASHTAG+"DEFAULT")
				);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public UserGroups createGroup(String adminCloudId, String email, String description, String name,
			List<String> members) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String acceeToken = getValidAccessToken(cloud.getCredential());

		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		GroupValue groupValue = null;
		String url = BASE_GROUP;
		String input = outlookHelper.createBodyForCreateGroup(name, description,email);
		for(int i=0;i<=1;i++) {

			try {
				String result = ConnectUtils.postResponse(url, acceeToken, input,cloud.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					groupValue = gson.fromJson(result, GroupValue.class);
					break;
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		if(groupValue!=null) {
			if(members!=null && !members.isEmpty()) {
				addMembersToGroup(cloud.getAdminCloudId(), members, groupValue.getId());
			}
			return ConvertionUtils.convertGroupToGroupEmailDetails(groupValue, members);
		}
		return null;
	}

	@Override
	public List<String> addMembersToGroup(String adminCloudId, List<String> members, String groupId) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String acceeToken = getValidAccessToken(cloud.getCredential());

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		List<String> listOfMembers = new ArrayList<>();
		GroupMembers groupValue = null;
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), adminCloudId);
		String url = String.format(ADD_MEMBER_GROUP,groupId);
		List<String>owners = new ArrayList<>();
		for(String member : members) {
			String _member = member.split(Const.HASHTAG)[0];
			JSONObject jsonObject=new JSONObject();
			if(mappedEmailDetails.get(_member)==null ) {
				continue;
			}
			try {
				if(member.split(Const.HASHTAG)[1].equalsIgnoreCase("OWNER")) {
					owners.add(member);
				}
			} catch (Exception e) {
			}
			jsonObject.put("@odata.id", "https://graph.microsoft.com/beta/directoryObjects/"+mappedEmailDetails.get(_member));
			String result = ConnectUtils.postResponse(url, acceeToken, jsonObject.toString(),cloud.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());

			if(StringUtils.isNotBlank(result)) {
				groupValue = gson.fromJson(result, GroupMembers.class);
				if(groupValue!=null) {
					groupValue.getValue().forEach(value->
						listOfMembers.add(value.getMail()+Const.HASHTAG+"DEFAULT")
					);
				}
			}
		}
		if(!owners.isEmpty()) {
			addOwnersToGroup(adminCloudId, owners, groupId);
		}
		return listOfMembers;
	}
	
	
	public List<String> addOwnersToGroup(String adminCloudId, List<String> members, String groupId) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String acceeToken = getValidAccessToken(cloud.getCredential());

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		List<String> listOfMembers = new ArrayList<>();
		GroupMembers groupValue = null;
//		if(!members.contains(cloud.getEmail())) {
//			members.add(cloud.getEmail());
//		}
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), adminCloudId);
		String url = String.format(ADD_OWNER_GROUP,groupId);
		for(String member : members) {
			member = member.split(Const.HASHTAG)[0];
			JSONObject jsonObject=new JSONObject();
			if(mappedEmailDetails.get(member)==null) {
				continue;
			}
			jsonObject.put("@odata.id", "https://graph.microsoft.com/beta/directoryObjects/"+mappedEmailDetails.get(member));
			String result = ConnectUtils.postResponse(url, acceeToken, jsonObject.toString(),cloud.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());

			if(StringUtils.isNotBlank(result)) {
				groupValue = gson.fromJson(result, GroupMembers.class);
				if(groupValue!=null) {
					groupValue.getValue().forEach(value->
						listOfMembers.add(value.getMail()+Const.HASHTAG+"DEFAULT")
					);
				}
			}
		}
		return listOfMembers;
	}

	public Value getEventMessage(CalenderFlags emailFlagsInfo) throws Exception {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		EmailList emailList = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return null; 
		}
		String mailFolder = emailFlagsInfo.getCalendar()==null?DEFAULT_MAILBOX:emailFlagsInfo.getCalendar();
		int count =0;
		try {
			String convId = emailFlagsInfo.getDestId();
			boolean tryFolders = true;
			while(tryFolders) {
				String url1 = null;
				if(emailList!=null && emailList.getOdataNextLink()!=null) {
					url1 = emailList.getOdataNextLink();
				}else if(emailList==null) {
					url1 = baseURL+"users/"+memberId+"/messages";
					url1 =url1+"?$expand=microsoft.graph.eventMessage/event&$filter=singleValueExtendedProperties/Any(ep: ep/id eq 'String 0x001A' and ep/value eq 'IPM.Schedule.Meeting.Request')";
				}else {
					return null;
				}
				count = count+1;
				String result = ConnectUtils.getResponse(url1, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				emailList = gson.fromJson(result, EmailList.class);
				if(!ObjectUtils.isEmpty(emailList) &&(emailList!=null && !emailList.getValue().isEmpty())) {
					for(Value value : emailList.getValue()) {
						if(convId.equals(value.getEvent().getId()) || emailFlagsInfo.getICalUId().equals(value.getEvent().getICalUId())) {
							return value;
						}
					}
				}else {
					Thread.sleep(1000);
					if(count >2 && (emailList!=null && emailList.getOdataNextLink()==null))
						throw new Exception("mail for the event not found in the folder :"+mailFolder);
				}
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}
		return null;
	}


	public boolean deleteEventMails(CalenderFlags emailFlagsInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String url = null;
		String mailFolder = emailFlagsInfo.getCalendar()==null?DEFAULT_MAILBOX:emailFlagsInfo.getCalendar();

		String result;
		try {
			url = String .format(SEND_MAIL, cloud.getMemberId(),mailFolder);
			url = url+"/"+emailFlagsInfo.getId();
			result = ConnectUtils.deleteResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isEmpty(result)) {
				return true;
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	public List<EmailUserSettings>createSharedCalender(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential());
		String url = String.format(CREATE_SHARED_CALENDAR, cloud.getMemberId());
		CalendarPermissions forwardSettings = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		forwardSettings = gson.fromJson(result, CalendarPermissions.class);
		List<EmailUserSettings> elements = new ArrayList<>();
		if(forwardSettings==null) {
			return Collections.emptyList();
		}
		EmailUserSettings emailSettings = new EmailUserSettings();
		emailSettings.setDelegates(true);
		emailSettings.setEmail(forwardSettings.getEmailAddress().getAddress());
		emailSettings.setRoles(forwardSettings.getAllowedRoles());
		elements.add(emailSettings);
		return elements;
	}

	@Override
	public UserGroups getSingleGroupEmailDetails(String adminCloudId, String email) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		GroupsList groupsList = null;
		UserGroups group = null;
		if(cloud.getCredential()==null) {
			cloud.setCredential(vendorOAuthCredentialRepo.findById(cloud.getEmail()+":"+cloud.getCloudName()));
		}
		String acceeToken = getValidAccessToken(cloud.getCredential());

		if(StringUtils.isBlank(acceeToken)) { 
			return group; 
		}
		String url = BASE_GROUP;
		url = url+"?$filter=startswith(mail,'"+email.split(Const.ATTHERATE)[0]+"')";
		do {
			if(groupsList!=null && groupsList.getOdataNextLink()!=null) {
				url = groupsList.getOdataNextLink();
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null,cloud.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isNotBlank(result)) {
				groupsList = gson.fromJson(result, GroupsList.class);
			}
			if(groupsList!=null && !groupsList.getValue().isEmpty()) {
				return ConvertionUtils.convertGroupToGroupEmailDetails(groupsList.getValue().get(0), null) ;
			}
		}while(groupsList!=null && groupsList.getOdataNextLink()!=null);
		return group;
	}
	

	private Map<String,String>getMemberDetails(String userId,String adminCloudId){
		return outlookHelper.getMemberDetails(userId, adminCloudId);
	}
	
	public List<EmailFlagsInfo>  creatDraftBatchRequest(List<EmailFlagsInfo>emailFlagsInfos,EmailFlagsInfo emailFlagsInfo) {
		return outlookHelper.creatDraftBatchRequest(emailFlagsInfos, emailFlagsInfo);
	}
	
	public List<EmailFlagsInfo>  createSendBatchRequest(List<EmailFlagsInfo>emailFlagsInfos,EmailFlagsInfo emailFlagsInfo) {
		return outlookHelper.creatSendBatchRequest(emailFlagsInfos, emailFlagsInfo);
	}

	@Override
	public List<Contacts> listContacts(ContactsFlagInfo contactsFlagInfo) {
		List<Contacts>contacts = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findOne(contactsFlagInfo.getCloudId());
		ContactsList contactsList = null;
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		String url = String.format(baseURL+GET_CONTACTS, memberId);
		String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			contactsList = gson.fromJson(result, ContactsList.class);
		}	
		if(contactsList!=null) {
			contactsFlagInfo.setNextPageToken(contactsList.getOdataNextLink());
			contactsList.getValue().forEach(value->{
				contacts.add(outlookHelper.convertContacts(value));
			});
		}
		return contacts;
	}

	@Override
	public List<CalenderInfo> getEventInstances(CalenderFlags emailFlagsInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		CalenderViews calenders = null;

		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		List<CalenderInfo>calendars = new ArrayList<>();
		do {
			String url = String.format(baseURL+GET_CALENDER_EVENT_INSTANCES, memberId,emailFlagsInfo.getCalendar(),emailFlagsInfo.getId(),emailFlagsInfo.getStartTime(),emailFlagsInfo.getEndTime());
			if(emailFlagsInfo.getNextPageToken()!=null) {
				url = emailFlagsInfo.getNextPageToken(); 
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isNotEmpty(result)) {
				calenders = gson.fromJson(result, CalenderViews.class);
			}	
			emailFlagsInfo.setNextPageToken(calenders.getOdataNextLink());
			calenders.getValue().forEach(calnder->{
				//if(calnder.getOrganizer().getEmailAddress().getAddress().equals(cloud.getEmail())) {
					CalenderInfo info= createInforViews(calnder,emailFlagsInfo.getId());
					calendars.add(info);
				//}
			});
		}while(calenders.getOdataNextLink()!=null);
		return calendars;
	}

}
