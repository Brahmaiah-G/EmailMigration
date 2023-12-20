package com.testing.mail.connectors.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.testing.mail.connectors.MailConnectors;
import com.testing.mail.connectors.google.data.AttachMents;
import com.testing.mail.connectors.google.data.Attachment;
import com.testing.mail.connectors.google.data.Attendee;
import com.testing.mail.connectors.google.data.CalendarsList;
import com.testing.mail.connectors.google.data.Connection;
import com.testing.mail.connectors.google.data.ContactInfo;
import com.testing.mail.connectors.google.data.DelegateSettings;
import com.testing.mail.connectors.google.data.DeltaMails;
import com.testing.mail.connectors.google.data.Domain;
import com.testing.mail.connectors.google.data.DomainsList;
import com.testing.mail.connectors.google.data.DriveAbout;
import com.testing.mail.connectors.google.data.EmailFolders;
import com.testing.mail.connectors.google.data.EntryPoint;
import com.testing.mail.connectors.google.data.EventItem;
import com.testing.mail.connectors.google.data.EventsList;
import com.testing.mail.connectors.google.data.FileMetadata;
import com.testing.mail.connectors.google.data.Filter;
import com.testing.mail.connectors.google.data.ForwardSettings;
import com.testing.mail.connectors.google.data.Group;
import com.testing.mail.connectors.google.data.GroupsList;
import com.testing.mail.connectors.google.data.History;
import com.testing.mail.connectors.google.data.Item;
import com.testing.mail.connectors.google.data.Label;
import com.testing.mail.connectors.google.data.MailBoxRules;
import com.testing.mail.connectors.google.data.MailThreads;
import com.testing.mail.connectors.google.data.MailValue;
import com.testing.mail.connectors.google.data.MembersList;
import com.testing.mail.connectors.google.data.Message;
import com.testing.mail.connectors.google.data.MessagesAdded;
import com.testing.mail.connectors.google.data.SendAs;
import com.testing.mail.connectors.google.data.SendAsSettings;
import com.testing.mail.connectors.google.data.ThreadMessages;
import com.testing.mail.connectors.google.data.TokenResponse;
import com.testing.mail.connectors.google.data.User;
import com.testing.mail.connectors.google.data.UserInfo;
import com.testing.mail.connectors.impl.helper.GmailHelper;
import com.testing.mail.connectors.management.utility.ConnectorUtility;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.contacts.dao.ContactsFlagInfo;
import com.testing.mail.contacts.entities.Contacts;
import com.testing.mail.contacts.entities.Emails;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.EmailUserSettings;
import com.testing.mail.dao.entities.ForwardingAddresses;
import com.testing.mail.dao.entities.RateLimitConfigurer;
import com.testing.mail.dao.entities.UserAutoForwarding;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.dao.entities.UserImap;
import com.testing.mail.dao.entities.UserPopSetting;
import com.testing.mail.dao.entities.UserVocation;
import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.exceptions.ContactCreationException;
import com.testing.mail.exceptions.MailCreationException;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;
import com.testing.mail.utils.ConnectUtils;
import com.testing.mail.utils.EmailUtils;
import com.testing.mail.utils.HttpUtils;
import com.testing.mail.utils.MappingUtils;
import com.testing.mail.utils.TimeUtils;
import com.testing.mail.utils.UploadSession;

import lombok.extern.slf4j.Slf4j;

/**Connector For The GOOGLE
 * <b>GOOGLE MAIL </b>
 * <p>
 * Parent Interface <b> {@link MailConnectors} </b>
 * </p>
 * @see com.testing.mail.connectors.MailConnectors &#64;MailConnectors
 */


@Slf4j
@Service
public class GMailConnector  implements MailConnectors {

	private  final String GET_MAILFOLDERS = "https://gmail.googleapis.com/gmail/v1/users/%s/labels?maxResults=20";
	private  final String GET_ATTACHMENTS = "https://gmail.googleapis.com/gmail/v1/users/%s/messages/%s/attachments/%s";
	private  final String GET_MESSAGES = "https://gmail.googleapis.com/gmail/v1/users/%s/messages?labelIds=%s";
	private  final String GET_THREADS = "https://gmail.googleapis.com/gmail/v1/users/%s/threads?labelIds=%s&maxResults=500";
	private  final String MOVE_MESSAGES = "https://gmail.googleapis.com/gmail/v1/users/%s/messages/%s/modify";
	private  final String GET_MAIL_BOX_RULES = "https://gmail.googleapis.com/gmail/v1/users/%s/settings/filters";
	private  final String CREATE_MAILFOLDER = "https://gmail.googleapis.com/gmail/v1/users/%s/labels";
	private  final String BASE_URL = "https://www.googleapis.com/admin/directory/v1/users";
	private  final int USERS_FETCH_LIMIT = 100;
	private  final String MAXRESULTS = "&maxResults=";
	private  final String GET_CALENDARS = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
	private  final String IMPORT_CALENDAR_EVENT = "https://www.googleapis.com/calendar/v3/calendars/%s/events/import?supportsAttachments=true&conferenceDataVersion=%s";//we are creating organizer as calling user not external for externaluser as organizer use /import
	private final String INSERT_CALENDAR_EVENT = "https://www.googleapis.com/calendar/v3/calendars/%s/events?supportsAttachments=true&conferenceDataVersion=%s";//we are creating organizer as calling user not external for externaluser as organizer use /import
	private final String GET_CALENDAR = "https://www.googleapis.com/calendar/v3/users/me/calendarList/%s";
	private  final String GET_LABEL = "https://gmail.googleapis.com/gmail/v1/users/%s/labels/%s";
	private  final String DEFAULT_MAIL_BOX = "INBOX";
	private  final String GET_CALENDAR_EVENTS = "https://www.googleapis.com/calendar/v3/calendars/%s/events";
	private  final String GET_CALENDAR_EVENT_INSTANCE = "https://www.googleapis.com/calendar/v3/calendars/%s/events/%s/instances?showDeleted=true";
	private  final String GET_CALENDAR_EVENTS_BY_TIME = "https://www.googleapis.com/calendar/v3/calendars/%s/events?timeMax=%s&timeMin=%s";
	private  final String GET_CALENDAR_EVENTS_page = "https://www.googleapis.com/calendar/v3/calendars/%s/events?pageToken=%s";
	private  final String UPDATE_CALENDAR_EVENT = "https://www.googleapis.com/calendar/v3/calendars/%s/events/%s?supportsAttachments=true";
	private final String HASHTAG = Const.HASHTAG;
	private final String CREATE_FILE = "https://www.googleapis.com/upload/drive/v2/files?uploadType=resumable&title=%s";
	private final String CREATE_CALENDAR = "https://www.googleapis.com/calendar/v3/calendars";
	private  final String DELTA_CHANGES= "https://gmail.googleapis.com/gmail/v1/users/%s/history?"
			+ "historyTypes=messageAdded&historyTypes=messageDeleted&historyTypes=labelAdded&historyTypes=labelRemoved&labelId=%s&startHistoryId=%s";
	private final String GET_GROUPS_ADMIN = "https://www.googleapis.com/admin/directory/v1/groups?customer=my_customer";
	private final String GET_SINGLE_GROUP_ADMIN = "https://www.googleapis.com/admin/directory/v1/groups/%s?customer=my_customer";
	private final String GET_GROUPS_ADMIN_PAGINATION = "https://www.googleapis.com/admin/directory/v1/groups?customer=my_customer&pageToken=%s";

	private final String CREATE_GROUP_ADMIN = "https://www.googleapis.com/admin/directory/v1/groups";
	private final String ADD_MEMEBERS_GROUP_ADMIN = "https://www.googleapis.com/admin/directory/v1/groups/%s/members";
	private final String GET_GROUP_MEMBERS = "https://www.googleapis.com/admin/directory/v1/groups/%s/members?maxResults=100";
	private final String GET_GROUP_MEMBERS_PAGINATION = "https://www.googleapis.com/admin/directory/v1/groups/%s/members?maxResults=100&pageToken=%s";
	private final String PAGE_TOKEN = "&pageToken=";
	Base64 base64 = new Base64();
	Map<String,String> eventDays = new HashMap<>();
	Map<String,String> mimeTypes = new HashMap<>();

	Map<String,String> loadDays() {
		eventDays =  GmailHelper.loadDays();
		return eventDays;
	}

	Map<String,String> loadMime(){
		mimeTypes.put("rar", "application/x-rar-compressed");
		mimeTypes.put("xls", "application/vnd.ms-excel");
		mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
		mimeTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

		return mimeTypes;
	}

	public GMailConnector() {
		loadDays();
		loadMime();
	}



	@Autowired
	RestTemplate restTemplate;
	@Autowired
	AppMongoOpsManager appMongoOpsManager;

	@Autowired
	private GmailHelper gmailHelper;
	@Autowired
	private CloudsRepoImpl cloudsRepoImpl;
	@Autowired
	private VendorOAuthCredentialImpl credentialImpl;

	@PostConstruct
	public final RateLimitConfigurer getConfigurer() {
		return appMongoOpsManager.findRateLimitConfig(CLOUD_NAME.GMAIL);
	}

	private Gson gson = new Gson() ;
	private Random random = new Random();
	private String NEWLINE = "\n";


	private String convertTime(String time,String timeZone) {
		return TimeUtils.convertGoogleTimeFormat(time,timeZone);
	}


	@Override
	public List<EmailFlagsInfo> getListOfMails(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		if(!emailFlagsInfo.isCopy()) {
			return getListOfMailsOnThreadsV2(emailFlagsInfo);
		}else {
			return getListOfMailsOnThreads(emailFlagsInfo);
		}
	}

	public List<EmailFlagsInfo> getListOfMailsOnThreads(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		MailThreads mailsList = null;
		//Timer timer = new Timer();
		long historyId = 0;
		Map<String,Long> threadOrder = new HashMap<>();
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds adminCloud = cloudsRepoImpl.findOne(cloud.getAdminCloudId());
		ConnectUtils.checkClouds(adminCloud);
		setTimeZone(cloud);
		List<EmailFlagsInfo> emailFlags = new ArrayList<>();
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		//int limit = getRateLimitCount("threads_list");
		Map<String,String>members = getMemberDetails(cloud.getUserId(), cloud.getAdminCloudId());
		String url = String.format(GET_THREADS, cloud.getMemberId(),emailFlagsInfo.getFolder());
		if(emailFlagsInfo.getNextPageToken()!=null) {
			url =url+PAGE_TOKEN+ emailFlagsInfo.getNextPageToken();
		}
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(StringUtils.isNotBlank(result)) {
			mailsList = gson.fromJson(result, MailThreads.class);
		}
		List<String> dups = new ArrayList<>();
		if(mailsList!=null && mailsList.getThreads()!=null &&!mailsList.getThreads().isEmpty()) {
			emailFlagsInfo.setNextPageToken(mailsList.getNextPageToken());
			for(com.testing.mail.connectors.google.data.Thread message : mailsList.getThreads()) {
				try {
					if(message.getId()!=null ) {
						ThreadMessages mailValues = null;
						acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
						String _url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/threads/"+message.getId();
						result = ConnectUtils.getResponse(_url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
						if(StringUtils.isNotBlank(result)) {
							mailValues = gson.fromJson(result, ThreadMessages.class);
						}
						if(mailValues!=null) {
							for(MailValue mailValue: mailValues.getMessages()) {
								if(dups.contains(mailValue.getId())) {
									continue;
								}
								dups.add(mailValue.getId());
								if(Long.valueOf(mailValue.getHistoryId())>historyId) {
									historyId = Long.valueOf(mailValue.getHistoryId());
								}
								String mailFolder = MappingUtils.checkGoogleMailFolder(mailValue.getLabelIds());
								if(mailFolder==null) {
									mailFolder = emailFlagsInfo.getFolder();
								}
								EmailFlagsInfo flagsInfo = createFlagsFromMails(mailValue,mailFolder,emailFlagsInfo.isStopCalendarSource(),adminCloud,members);
								if(flagsInfo!=null) {
									if(threadOrder.containsKey(flagsInfo.getThreadId())) {
										threadOrder.put(flagsInfo.getThreadId(), threadOrder.get(flagsInfo.getThreadId())+1);
									}else {
										threadOrder.put(flagsInfo.getThreadId(), 0L);
									}
									flagsInfo.setFolder(mailFolder);
									flagsInfo.setOrder(threadOrder.get(flagsInfo.getThreadId()));
									if(mailValue.getLabelIds().contains(MappingUtils.MAIL_FOLDERS.TRASH.name())) {
										flagsInfo.setFolder(MappingUtils.MAIL_FOLDERS.TRASH.name());
									}
									emailFlags.add(flagsInfo);
								}
							}
						}
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
					throw e;
				}
			}
		}
		emailFlagsInfo.setParentFolderId(""+historyId);
		threadOrder.clear();
		dups.clear();
		return emailFlags;
	}
	
	
	public List<EmailFlagsInfo> getListOfMailsOnThreadsV2(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		MailThreads mailsList = null;
		//Timer timer = new Timer();
		long historyId = 0;
		Map<String,Long> threadOrder = new HashMap<>();
		Map<String,EmailFlagsInfo> threadOrderMap = new HashMap<>();
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds adminCloud = cloudsRepoImpl.findOne(cloud.getAdminCloudId());
		ConnectUtils.checkClouds(adminCloud);
		setTimeZone(cloud);
		List<EmailFlagsInfo> emailFlags = new ArrayList<>();
		if(cloud.getCredential()==null) {
			VendorOAuthCredential cred = new VendorOAuthCredential();
			cred.setId(cloud.getEmail()+":"+cloud.getCloudName().name());
			cloud.setCredential(cred);
		}
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		//int limit = getRateLimitCount("threads_list");
		Map<String,String>members = getMemberDetails(cloud.getUserId(), cloud.getAdminCloudId());
		String url = String.format(GET_THREADS, cloud.getMemberId(),emailFlagsInfo.getFolder());
		if(emailFlagsInfo.getNextPageToken()!=null) {
			url =url+PAGE_TOKEN+ emailFlagsInfo.getNextPageToken();
		}
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(StringUtils.isNotBlank(result)) {
			mailsList = gson.fromJson(result, MailThreads.class);
		}
		List<String> dups = new ArrayList<>();
		if(mailsList!=null && mailsList.getThreads()!=null &&!mailsList.getThreads().isEmpty()) {
			emailFlagsInfo.setNextPageToken(mailsList.getNextPageToken());
			for(com.testing.mail.connectors.google.data.Thread message : mailsList.getThreads()) {
				try {
					if(message.getId()!=null ) {
						ThreadMessages mailValues = null;
						acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
						String _url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/threads/"+message.getId();
						result = ConnectUtils.getResponse(_url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
						if(StringUtils.isNotBlank(result)) {
							mailValues = gson.fromJson(result, ThreadMessages.class);
						}
						if(mailValues!=null) {
							for(MailValue mailValue: mailValues.getMessages()) {
								if(threadOrder.containsKey(mailValue.getThreadId())) {
									threadOrder.put(mailValue.getThreadId(), threadOrder.get(mailValue.getThreadId())+1);
								}else {
									threadOrder.put(mailValue.getThreadId(), 0L);
								}
								if(dups.contains(mailValue.getId())) {
									continue;
								}
								dups.add(mailValue.getId());
								if(Long.valueOf(mailValue.getHistoryId())>historyId) {
									historyId = Long.valueOf(mailValue.getHistoryId());
								}
								String mailFolder = MappingUtils.checkGoogleMailFolder(mailValue.getLabelIds());
								if(mailFolder==null) {
									mailFolder = emailFlagsInfo.getFolder();
								}
								EmailFlagsInfo flagsInfo = createFlagsFromMails(mailValue,emailFlagsInfo.getFolder(),emailFlagsInfo.isStopCalendarSource(),adminCloud,members);
								if(flagsInfo!=null) {
									if(!flagsInfo.getFrom().equals(cloud.getEmail()) && members.containsKey(flagsInfo.getFrom())) {
										if(threadOrder.containsKey(flagsInfo.getThreadId())) {
											Long tCount = threadOrder.get(flagsInfo.getThreadId());
											if(tCount<=0) {
												break;
											}
										}else {
											break;
										}
									}
									
									if(threadOrderMap.containsKey(flagsInfo.getThreadId())) {
										String threadFrom = threadOrderMap.get(flagsInfo.getThreadId()).getFrom();
										if(!cloud.getEmail().equals(threadFrom) && members.containsKey(threadFrom)) {
											break;
										}
									}else {
										threadOrderMap.put(flagsInfo.getThreadId(), flagsInfo);
									}
									flagsInfo.setFolder(mailFolder);
									if(!mailFolder.equalsIgnoreCase(emailFlagsInfo.getFolder()) && (threadOrder.get(flagsInfo.getThreadId())==null || threadOrder.get(flagsInfo.getThreadId())<=0)) {
										continue;
									}
									flagsInfo.setOrder(threadOrder.get(flagsInfo.getThreadId()));
									if(mailValue.getLabelIds().contains(MappingUtils.MAIL_FOLDERS.TRASH.name())) {
										flagsInfo.setFolder(MappingUtils.MAIL_FOLDERS.TRASH.name());
									}
									if(MappingUtils.MAIL_FOLDERS.SENT.name().equals(mailFolder) && flagsInfo.getFrom().equals(cloud.getEmail())) {
										emailFlags.add(flagsInfo);
									}else if(!MappingUtils.MAIL_FOLDERS.SENT.name().equals(mailFolder)) {
										emailFlags.add(flagsInfo);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
					throw e;
				}
			}
		}
		emailFlagsInfo.setParentFolderId(""+historyId);
		threadOrder.clear();
		dups.clear();
		return emailFlags;
	}

	private void setTimeZone(Clouds cloud) {
		try {
			if(cloud.getTimeZone()==null && cloud.isCalendarEnabled()) {
				String zone = getTimeZone(cloud.getId());
				cloud.setTimeZone(zone);
			}
		} catch (Exception e) {
			cloud.setCalendarEnabled(false);
			log.error(ExceptionUtils.getStackTrace(e));
			cloud.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}finally{
			cloudsRepoImpl.save(cloud);
		}
	}

	@Override
	public List<EmailFlagsInfo> getListOfMailFolders(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		List<EmailFlagsInfo> mailFolders = new ArrayList<>();
		EmailFolders emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		setTimeZone(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String memberId=cloud.getMemberId();

		String url = String.format(GET_MAILFOLDERS, memberId);
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(!StringUtils.isBlank(result)) { 
			emailFolders = gson.fromJson(result, EmailFolders.class); 

			if(emailFolders!=null ) { 
				for(Label label : emailFolders.getLabels()) { 
					if(getSelectedLabelFolders(label)) {
						continue; 
					}
					EmailFlagsInfo info = null;
					if(StringUtils.isNotBlank(emailFlagsInfo.getFolder())) {
						//For under folders name will be in the form of parentFolder/ChildFolder so splitting and getting the last foldername as it might contain inner and inner
						String label_name = label.getName();
						if(label.getName().split("/").length>1) {
							label_name = label.getName().split("/")[label.getName().split("/").length-1];
						}
						if(emailFlagsInfo.getFolder().equalsIgnoreCase(label_name)) {
							mailFolders.clear();
							info = createMailFolderInfo(label);
							mailFolders.add(info);
							return mailFolders;
						}
					}else {
						info = createMailFolderInfo(label);
						mailFolders.add(info);
					}
				}
			} 
			return mailFolders;
		}
		return Collections.emptyList();
	}


	private EmailFlagsInfo createMailFolderInfo(Label label) {
		return gmailHelper.createMailFolderInfo(label);
	}



	private EmailFlagsInfo createMailFolderInfo(EmailInfo label) {
		EmailFlagsInfo info = new EmailFlagsInfo(); 
		if(label.getMailFolder().split("/").length>1) {
			String parent = label.getMailFolder().replace(label.getMailFolder().substring(label.getMailFolder().lastIndexOf("/"),label.getMailFolder().length()), "");
			String name = label.getMailFolder().replace(parent+"/", "");
			label.setMailFolder(name);
			info.setSubFolder(true);
			info.setParentFolderId(parent.split("/").length>1?parent.split("/")[1]:parent);
		}else {
			info.setParentFolderId("/");
		}
		info.setName(label.getMailFolder());
		info.setId(label.getId());
		info.setFolder(label.getMailFolder());
		info.setMailFolder(true);
		return info;
	}

	private boolean getSelectedLabelFolders(Label label) {
		if(MappingUtils.MAIL_FOLDERS.INBOX.name().equals(label.getName()) || MappingUtils.MAIL_FOLDERS.DRAFT.name().equals(label.getName()) || MappingUtils.MAIL_FOLDERS.TRASH.name().equals(label.getName()) || MappingUtils.MAIL_FOLDERS.SPAM.name().equals(label.getName())
				|| MappingUtils.MAIL_FOLDERS.SENT.name().equals(label.getName())){
			return false;
		}else if(label.getMessageListVisibility()!=null && label.getMessageListVisibility().equals("hide")){
			return true;
		}else if(label.getName().equalsIgnoreCase(MappingUtils.MAIL_FOLDERS.UNREAD.name()) || label.getName().equalsIgnoreCase(MappingUtils.MAIL_FOLDERS.STARRED.name())) {
			return true;
		}
		return false;
	}

	@Override
	public EmailFlagsInfo getMailById(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		return null;
	}

	/**For Creating a mailFolder apart from DefaultMailFolders(default ex:Inbox,Sent)
	 */
	@Override
	public EmailInfo createAMailFolder(EmailFlagsInfo emailFlagsInfo) throws MailCreationException {
		Label createdLabel = null;
		EmailInfo emailInfo = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String accessToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String memberId = cloud.getMemberId();
		String url = String.format(CREATE_MAILFOLDER,memberId); 
		if(emailFlagsInfo.getFolder()==null) { 
			return null; 
		} 
		JSONObject mailFolder = new JSONObject();
		try {
			String name = emailFlagsInfo.getFolder();
			if(emailFlagsInfo.isSubFolder()) {
				name = emailFlagsInfo.getId()+"/"+emailFlagsInfo.getFolder();
			}
			if(name.startsWith("/")) {
				name = name.substring(1);
			}
			mailFolder.put("name", name);
			mailFolder.put("messageListVisibility", "show");
			mailFolder.put("labelListVisibility", "labelShow");
			mailFolder.put("type", "user");
			String result = ConnectUtils.postResponse(url,accessToken,mailFolder.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if(result!=null) {
				createdLabel = gson.fromJson(result,Label.class); 
			} 
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e)); 
		}
		if(createdLabel!=null) {
			emailInfo = new EmailInfo();
			emailInfo.setId(createdLabel.getId());
			emailInfo.setMailFolder(createdLabel.getName());
		}else {
			return null;
		}
		return emailInfo; 
	}

	/**	
	 *For Sending Email to Gmail in RAW Encoded format Base64 and urlSafe with padding 
	 *follow the documentation for further information  
	 *@see Documentations <a href=https://developers.google.com/gmail/api/reference/rest/v1/users.messages/insert>Documentation</a>
	 */
	@Override
	public EmailInfo sendEmail(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {
		if(emailFlagsInfo==null) { 
			return null; 
		}
		EmailInfo emailInfo = null;
		Message gmail = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		setTimeZone(cloud);
		ConnectUtils.checkClouds(cloud);
		emailFlagsInfo.setTimeZone(cloud.getTimeZone());    
		String mailFolder = emailFlagsInfo.getFolder();
		String url = String.format(GET_MESSAGES,cloud.getMemberId(),mailFolder);
		String accessToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		if(StringUtils.isBlank(accessToken)) {
			return null; 
		}

		if(mailFolder.equals(MappingUtils.MAIL_FOLDERS.DRAFT.name())) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}

		if(emailFlagsInfo.getDestId()!=null ) {
			emailInfo = new EmailInfo();
			emailInfo.setMailFolder(emailFlagsInfo.getLabels().get(0));
			emailInfo.setId(emailFlagsInfo.getDestId());
			return moveEmails(emailFlagsInfo, emailInfo);
		}

		url = url+"&internalDateSource=dateHeader";
		String from = EmailUtils.FROM+":"+emailFlagsInfo.getFrom(); 
		String to = EmailUtils.TO+":";
		String replyTo = "";
		String bcc = "";
		String cc = "";
		String subject = EmailUtils.SUBJECT+emailFlagsInfo.getSubject(); 
		StringBuffer rawData = new StringBuffer();
		String _date  = convertTime(emailFlagsInfo.getSentTime(),emailFlagsInfo.getTimeZone()); 
		String date = EmailUtils.DATE+_date;
		if(emailFlagsInfo.getTo()!=null && !emailFlagsInfo.getTo().isEmpty()) {
			String listOfTos = null; 
			for(String toMail :emailFlagsInfo.getTo()) {
				if(listOfTos!=null) { 
					listOfTos = listOfTos+","+toMail; 
				}else { 
					listOfTos = toMail; 
				} 
			} 
			if(StringUtils.isNotBlank(listOfTos)) {
				to =to +listOfTos; 
			}
		}

		cc =EmailUtils.CC+":"+ setAddresses(emailFlagsInfo.getCc()); 
		replyTo = EmailUtils.REPLY_TO+":"+setAddresses(emailFlagsInfo.getReplyTo());
		bcc = EmailUtils.BCC+":"+setAddresses(emailFlagsInfo.getBcc());

		String baseContentType = "Content-Type";
		String overallBounday = ""+Math.abs(random.nextLong()); 
		if(emailFlagsInfo.isHtmlContent() && emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty() && emailFlagsInfo.isHadAttachments()) { 
			//Check before changing the content type it might effect in DRAFT not in INBOX # Bug EC-721 started
			baseContentType = baseContentType+":multipart/mixed;"+EmailUtils.BOUNDARY+overallBounday+NEWLINE; 
			//#Bug EC-721 Ended
		} else {
			baseContentType = baseContentType+":multipart/alternative;"+EmailUtils.BOUNDARY+overallBounday+NEWLINE;
		}
		rawData.append(EmailUtils.MIME_VERSION).append(NEWLINE).append(date).append(NEWLINE).append(subject).append(NEWLINE).append(from).append(NEWLINE).append(to).append(NEWLINE);
		rawData.append(cc).append(NEWLINE);
		rawData.append(bcc).append(NEWLINE);
		rawData.append(replyTo).append(NEWLINE);

		// : for larger files the link is appending on the text so need to replace that content inside it with the destination on as anchor tag #Done
		Optional<Map<String, String>>aLinks = Optional.ofNullable(HttpUtils.getAnchorTags(emailFlagsInfo.getHtmlMessage()));
		if(emailFlagsInfo.getHtmlMessage()==null || (emailFlagsInfo.getHtmlMessage()!=null && (emailFlagsInfo.getHtmlMessage().isEmpty() || emailFlagsInfo.getHtmlMessage().equals("null")))) {
			emailFlagsInfo.setHtmlMessage(emailFlagsInfo.getBodyPreview());
		}
		rawData.append(baseContentType).append(NEWLINE).append("--"+overallBounday).append(NEWLINE);
		String htmlBoundary = ""+Math.abs(random.nextLong());
		if(!ConnectorUtility.isEmptyOrNullString(emailFlagsInfo.getHtmlMessage()) && emailFlagsInfo.isHadAttachments()) {
			rawData.append(EmailUtils.CONTENT_TYPE+":").append("multipart/alternative;").append(EmailUtils.BOUNDARY+htmlBoundary).append(NEWLINE+"--"+htmlBoundary+NEWLINE);
		}else {
			htmlBoundary = overallBounday;
		}
		boolean hadAttach = false;
		if(emailFlagsInfo.getAttachments()!=null) {
			for(AttachmentsData attachments : emailFlagsInfo.getAttachments()) {
				if(attachments.getSize()<Const.GMAIL_ATTACHMENT_LIMIT && !attachments.isInline()) {
					hadAttach = true;
					continue;
				}
				attachments.setCompleted(true);
				AttachmentsData data = uploadLargeFile(emailFlagsInfo, attachments);
				if(data!=null && aLinks.isPresent()) {
					Map<String,String> links = aLinks.get();
					for(Map.Entry<String, String> entry : links.entrySet()) {
						if(entry.getKey().contains(attachments.getId()) || entry.getValue().equals(data.getName())) {
							emailFlagsInfo.setHtmlMessage(emailFlagsInfo.getHtmlMessage().replace(StringEscapeUtils.escapeHtml4(entry.getKey()),StringEscapeUtils.escapeHtml4(data.getOdataType())));
						}
					}
				}
			}
			emailFlagsInfo.setHadAttachments(hadAttach);
		}

		if(emailFlagsInfo.getHtmlMessage()!=null) { 
			rawData.append(EmailUtils.CONTENT_TYPE+":"+"text/html; charset=\"UTF-8\""+NEWLINE+NEWLINE+emailFlagsInfo.getHtmlMessage()+NEWLINE+"--"+htmlBoundary+"--"+NEWLINE);
		}
		if(emailFlagsInfo.isHadAttachments() && emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
			for(AttachmentsData attachments : emailFlagsInfo.getAttachments()) {
				try {
					if(!attachments.isCompleted()) {
						//check for the largeFile appending in the  sendEmail need to check # done uploading it to drive and adding a anchor tag for that
						if(attachments.getSize()>=Const.GMAIL_ATTACHMENT_LIMIT) {
							continue;
						}else{
							attachments.setCompleted(true);
							rawData.append("--"+overallBounday+NEWLINE+EmailUtils.CONTENT_TYPE+":"+attachments.getContentType()+"; name="+attachments.getName()+NEWLINE+
									EmailUtils.CONTENT_DISPOSITION+EmailUtils.ATTACHMENT+"; fileName="+attachments.getName()+NEWLINE+EmailUtils.CONTENT_TRANSFER_ENCODING+NEWLINE); 
							rawData.append(attachments.getContentBytes()+NEWLINE);
						}
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
			//rawData.append("--"+overallBounday+"--");//removing for drafts image issue
		}

		if(emailFlagsInfo.getHtmlMessage()!=null && !htmlBoundary.equals(overallBounday)){ 
			rawData.append("--"+overallBounday+"--"); 
		}

		JSONObject json = new JSONObject();
		try {
			//Encoding the whole String Builder Base64
			//Encoding check the Rawdata format if it changes then there will be issue in drafts not in Other folders
			String encodedData = EmailUtils.encodeMimeTextToBase64Binary(rawData.toString()).toString();
			json.put("raw", encodedData);
			JSONArray array = new JSONArray();
			array.put(mailFolder);
			if(!emailFlagsInfo.isRead()) {
				array.put("UNREAD");
			}
			if(emailFlagsInfo.isFlagged()) {
				array.put("STARRED");
			}
			if(emailFlagsInfo.getImportance()!=null && emailFlagsInfo.getImportance().equalsIgnoreCase("important")) {
				array.put("IMPORTANT");
			}
			json.put("labelIds", array); 
			if(emailFlagsInfo.getThreadId()!=null) {
				json.put("threadId",emailFlagsInfo.getThreadId()); 
			} 
		} catch (Exception e) { 
			log.error(ExceptionUtils.getStackTrace(e));
		}

		String response = ConnectUtils.postResponse(url, accessToken, json.toString(),cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(StringUtils.isNotBlank(response)) {
			try { 
				gmail =gson.fromJson(response, Message.class); 
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		if(gmail!=null) {
			emailInfo = new EmailInfo();
			emailInfo.setThreadId(gmail.getThreadId());
			emailInfo.setId(gmail.getId()); 
		} 
		return emailInfo;
	}

	private String setAddresses(List<String>addresses) {
		if(addresses!=null && !addresses.isEmpty()) {
			return addresses.stream().filter(StringUtils::isNotBlank)
					.collect(Collectors.joining(","));
		}
		return "";
	}

	@Override
	public Clouds getAdminDetails(ConnectFlags connectFlags) {

		String url = BASE_URL + "/" + connectFlags.getEmailId();
		Clouds adminUserInfo = new Clouds();
		try {
			String result = ConnectUtils.getResponse(url, connectFlags.getAccessToken(), null, null, CLOUD_NAME.GMAIL,null);

			User user = gson.fromJson(result, User.class);
			if (user.getIsAdmin() == Boolean.FALSE) {
				return null;
			}
			String email = user.getPrimaryEmail();
			String displayName = user.getName().getFullName() != null ? user.getName().getFullName() : "";
			adminUserInfo.setEmail(email);
			adminUserInfo.setName(displayName);
			adminUserInfo.setMemberId(user.getId());
			adminUserInfo.setAdminMemberId(user.getCustomerId());

			adminUserInfo.setAdmin(user.getIsAdmin());
		} catch (Exception e) {
			adminUserInfo = null;
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return adminUserInfo;

	}

	@Override
	public List<Clouds> getUsersList(ConnectFlags connectFlags) {
		List<Clouds> users = new ArrayList<>();
		MembersList membersList = null;
		String url = null;
		Clouds cloud = cloudsRepoImpl.findAdmin(connectFlags.getAdminMemberId(),connectFlags.getUserId());
		ConnectUtils.checkClouds(cloud);

		do {
			url = membersList == null
					? BASE_URL + "?customer=my_customer&viewType=admin_view&maxResults=" + USERS_FETCH_LIMIT
							: BASE_URL + "?customer=my_customer&viewType=admin_view" + MAXRESULTS + USERS_FETCH_LIMIT
							+ PAGE_TOKEN + membersList.getNextPageToken();

			String result = ConnectUtils.getResponse(url, connectFlags.getAccessToken(), null, cloud.getAdminEmailId(),CLOUD_NAME.GMAIL,cloud.getId());

			try {
				membersList = gson.fromJson(result, MembersList.class);
				if (membersList != null) {
					membersList.getUsers().forEach(user -> {
						try {
							Clouds saaSUser = new Clouds();
							if (!user.getPrimaryEmail().equals(connectFlags.getEmailId())) {
								saaSUser.setName(user.getName().getFullName());
								saaSUser.setMemberId(user.getId());
								saaSUser.setEmail(user.getPrimaryEmail());
								saaSUser.setActive(!user.getSuspended() && user.getIsMailboxSetup());
								if(Boolean.TRUE.equals(user.getSuspended())){
									saaSUser.setErrorDescription(user.getSuspensionReason());
								}
								if(Boolean.FALSE.equals(user.getIsMailboxSetup())) {
									saaSUser.setMailBoxStatus(ExceptionConstants.MAILBOXNOT_FOUND);
								}
								users.add(saaSUser);
							}
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
					});
				}

			} catch (Exception e) {
				log.error("Exception while fetching member details   ::" + ExceptionUtils.getStackTrace(e));
			}

		} while (membersList != null && StringUtils.isNotBlank(membersList.getNextPageToken()));
		return users;
	}

	public String getUserInfo(String accessToken) {
		String url = "https://www.googleapis.com/oauth2/v1/userinfo";
		String result = ConnectUtils.getResponse(url, accessToken, null, null, CLOUD_NAME.GMAIL,null);

		if (StringUtils.isNotBlank(result)) {
			try {
				UserInfo user = gson.fromJson(result, UserInfo.class);
				if (user != null && user.getVerifiedEmail()) {
					return user.getEmail();
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public String getDeltaChangeId(EmailFlagsInfo connectFlags) {
		return null;
	}

	/**
	 *Getting the Attachments From the cloud based on Message/Event Id
	 */
	@Override
	public List<AttachmentsData> getAttachments(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {

		AttachMents emailAttachments = null;
		List<AttachmentsData> list = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String memberId=cloud.getMemberId();
		String mailFolder = emailFlagsInfo.getFolder();
		if(StringUtils.isBlank(mailFolder)) {
			mailFolder = DEFAULT_MAIL_BOX;
		}
		mailFolder = mailFolder.toUpperCase();
		if(emailFlagsInfo.getAttachments()==null) {
			return Collections.emptyList();
		}
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String regularExpression = "^(?:[0-9]+[a-zA-Z#$_-]|[a-zA-Z]+[0-9])[a-zA-Z0-9#$_-]*$";
		// alredy fetched attachments and in that content is not there like data bytes so getting that using the attachMent Id
		for(AttachmentsData data:  emailFlagsInfo.getAttachments()) {
			String mime = fileNameMap.getContentTypeFor(data.getName()==null ? "":data.getName());
			if(data.getName()!=null && mime==null && mimeTypes.containsKey(data.getName().substring(data.getName().lastIndexOf(".")+1,data.getName().length()))) {
				mime = mimeTypes.get(data.getName().substring(data.getName().lastIndexOf(".")+1,data.getName().length()));
			}
			boolean inlineData = false;
			if(data.isInline() && data.getSize()==Const.GMAIL_ATTACHMENT_LIMIT) {
				UriComponents comp = UriComponentsBuilder.fromUriString(data.getId()).build();
				Pattern p = Pattern.compile(regularExpression);
				for(String id : comp.getPathSegments()) {
					Matcher m = p.matcher(id);
					if(m.matches() || 	m.find()) {
						data.setId(id);
						inlineData = true;
						break;
					}
				}
			}
			if(emailFlagsInfo.isEvents() || inlineData || data.isInline()) {
				AttachmentsData content = getFileMetadata(data.getId(), emailFlagsInfo.getCloudId());
				if(ObjectUtils.isEmpty(content)) {
					log.info("Contetn for the attachment fetched is null=="+emailFlagsInfo.getCloudId()+"--"+data.getId());
					continue;
				}
				AttachmentsData attachmentsData = new AttachmentsData();
				attachmentsData.setContentType(data.getContentType()==null ?(mime==null ? content.getContentType() : mime) : data.getContentType());
				if(data.getContentType()!=null) {
					if(content.getContentType().equals("application/vnd.google-apps.presentation")) {
						data.setName(data.getName()+".pptx");
					}else if(content.getContentType().equals("application/vnd.google-apps.spreadsheet")) {
						data.setName(data.getName()+".xlsx");
					}else if(content.getContentType().equals("application/vnd.google-apps.document")) {
						data.setName(data.getName()+".docx");
					}
				}
				attachmentsData.setName((StringUtils.isBlank(data.getName()) || data.getName().equals("null"))? content.getName():data.getName());
				attachmentsData.setContentBytes(content.getContentBytes());
				attachmentsData.setSize(content.getSize());
				attachmentsData.setLargeFile(attachmentsData.getSize()>Const.ATTACHMENT_LIMIT);
				attachmentsData.setInline(data.isInline());
				attachmentsData.setContentType(StringUtils.isEmpty(attachmentsData.getContentType()) || attachmentsData.getContentType().equals("null") ? content.getContentType() : attachmentsData.getContentType());
				attachmentsData.setEncoded(true);
				attachmentsData.setId(data.getId());
				attachmentsData.setName(attachmentsData.getName().replace(":", "-"));
				list.add(attachmentsData);
			}else {
				// check in the mail migration setting the id as sourceId and for new attachments
				String url = String.format(GET_ATTACHMENTS, memberId,emailFlagsInfo.getId(),data.getId());
				String result = ConnectUtils.getResponse(url, acceeToken, null,cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
				if(result!=null) {
					try {
						emailAttachments = gson.fromJson(result, AttachMents.class);
					}
					catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e)); 
					}
					if(emailAttachments!=null && !emailAttachments.getData().isEmpty()) {
						try {
							AttachmentsData attachmentsData = new AttachmentsData();
							attachmentsData.setId(data.getId());
							attachmentsData.setEncoded(true);
							if(emailFlagsInfo.isLargeFile() && emailAttachments.getSize()>Const.ATTACHMENT_LIMIT) {

								attachmentsData.setContentType(data.getContentType());
								attachmentsData.setName(data.getName());
								attachmentsData.setContentBytes(emailAttachments.getData());
								attachmentsData.setSize(emailAttachments.getSize());
								attachmentsData.setLargeFile(true);
								attachmentsData.setInline(data.isInline());
							}else if(!emailFlagsInfo.isLargeFile()){
								String decode = emailAttachments.getData();
								if(emailAttachments.getSize()>Const.ATTACHMENT_LIMIT) {
									attachmentsData.setLargeFile(true);
									if(emailFlagsInfo.isGCombo()) {
										String _decode = decode.replace("-", "+");
										_decode = _decode.replace("_", "/");//TODO: need to check as we are encoding and decoding it consumes more memory.
										//added for google uploading when it is replaced if we are directly passing the bytes the data being corrupted once check before change.
										decode = _decode;
									}
									//For largeFile replacing is not required for *OUTLOOK* as we are uploading in the form of a file
								}else {
									decode = decode.replace("-", "+");
									decode = decode.replace("_", "/");//TODO: need to check as we are encoding and decoding it consumes more memory.
									//other encodings are not supported while uploading content in onedrive it is not acception once check before changing
								}
								attachmentsData.setContentType(data.getContentType());
								attachmentsData.setName(data.getName());
								attachmentsData.setContentBytes(decode);
								attachmentsData.setInline(data.isInline());
								attachmentsData.setSize(emailAttachments.getSize());
							}
							list.add(attachmentsData);
						}catch(Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
					}
				}
			}
		}
		return list;
	}

	/** Getting Attachments using downloading file attaches from drive */

	public AttachmentsData getFileMetadata(String fileId,String cloudId){
		Clouds cloud = cloudsRepoImpl.findOne(cloudId);
		AttachmentsData attachmentsData = new AttachmentsData();
		FileMetadata fileMetadata = null;
		if(cloud==null)
			throw new MailMigrationException("cloud not available");
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://www.googleapis.com/drive/v2/files/"+fileId;
		String result = null;
		try {
			result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		} catch (Exception e2) {
			log.error(ExceptionUtils.getStackTrace(e2));
		}
		if(StringUtils.isNotEmpty(result)) {
			fileMetadata = gson.fromJson(result, FileMetadata.class);
			attachmentsData.setId(fileMetadata.getId());
			attachmentsData.setContentType(fileMetadata.getMimeType());
			attachmentsData.setOdataType(fileMetadata.getAlternateLink());
			attachmentsData.setName(fileMetadata.getTitle());
			attachmentsData.setSize(fileMetadata.getFileSize());
		}else {
			return null;
		}
		String EXPORT_URL = "https://www.googleapis.com/drive/v2/files/"+fileId+"/export?mimeType=";
		String downloadUrl = null;
		String mimeType = null;
		String defaultDownloadUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";

		if(fileMetadata!=null) {

			if(StringUtils.isNotBlank(fileMetadata.getDownloadUrl())){
				downloadUrl = fileMetadata.getDownloadUrl();
				EXPORT_URL = null;
			}else if(fileMetadata!=null && fileMetadata.getMimeType()!=null && fileMetadata.getMimeType().equals("application/vnd.google-apps.shortcut")) {
				return null;
			}else if(fileMetadata != null && fileMetadata.getExportLinks()!= null){
				String link = null;
				Set<String> keys = fileMetadata.getExportLinks().keySet();
				for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
					String string = iterator.next();
					if(string!=null) {
						if(string.contains("openxmlformats")){
							mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
							link = fileMetadata.getExportLinks().get(string);
							break;
						}else if(fileMetadata.getMimeType()!=null && fileMetadata.getMimeType().equalsIgnoreCase("application/vnd.google-apps.drawing")){
							if(string.contains("image/png")){
								link = fileMetadata.getExportLinks().get(string);
								break;
							}
						}
					}
				}
				if(StringUtils.isNotBlank(link)){
					downloadUrl = link;
					log.info("link : "+link);
				}
				if(fileMetadata!=null && fileMetadata.getMimeType()!=null && fileMetadata.getMimeType().equals("application/vnd.google-apps.spreadsheet")){
					mimeType ="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
				}else if(fileMetadata!=null && fileMetadata.getMimeType()!=null && fileMetadata.getMimeType().equals( "application/vnd.google-apps.presentation")){
					mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
				}
			}

		}

		if(mimeType!=null){
			EXPORT_URL = EXPORT_URL+mimeType;
			downloadUrl = EXPORT_URL;
		}
		if(StringUtils.isBlank(downloadUrl)){
			downloadUrl = defaultDownloadUrl;
		}

		InputStream stream;
		try {
			stream = ConnectUtils.executeDownloadRequest(downloadUrl, acceeToken, fileMetadata.getId(), defaultDownloadUrl, EXPORT_URL);
		} catch (Exception e1) {
			log.error(ExceptionUtils.getStackTrace(e1));
			return null;
		}
		if(stream!=null) {
			try {
				log.info("==Downloaded attachment =="+fileMetadata.getTitle());
				Base64 x = new Base64();
				attachmentsData.setContentBytes(x.encodeAsString(IOUtils.toByteArray(stream)));
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			} 
		}
		return attachmentsData;
	}

	/**
	 * Getting MailFolders like <b>INBOX</b>,<b>DRAFTS</b>
	 */
	@Override
	public EmailInfo getLabel(EmailFlagsInfo emailFlagsInfo) {
		log.info("Getting the label for the gmail=="+emailFlagsInfo.getCloudId());
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String mailFolder = emailFlagsInfo.getFolder();
	//	mailFolder = mailFolder.replace(" ", "");
		String memberId = cloud.getMemberId();
		String url =String.format(GET_LABEL, memberId,mailFolder);
		Label label = null;
		try {
			String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if(StringUtils.isNotEmpty(result)) {
				label = gson.fromJson(result, Label.class);
			}else {
				return null;
			}
		} catch (Exception e) {
			//Added for custom folders not fetching based on name So Searching in the ListOfMailFolders
			List<EmailFlagsInfo> flagsInfo = getListOfMailFolders(emailFlagsInfo);
			if(flagsInfo!=null && !flagsInfo.isEmpty()) {
				EmailInfo emailInfo = new EmailInfo();
				emailInfo.setId(flagsInfo.get(0).getId());
				emailInfo.setMailFolder(flagsInfo.get(0).getName());
				return emailInfo;
			}else {
				return null;
			}
		} 
		EmailInfo emailInfo = new EmailInfo();
		emailInfo.setFolder(true);
		emailInfo.setId(label.getId());
		emailInfo.setMailFolder(label.getName());
		return emailInfo;
	}


	public EmailInfo checkUserMailBoxStatus(String accessToken,String memberId) {

		String url =String.format(GET_LABEL, memberId,DEFAULT_MAIL_BOX);
		Label label = null;
		try {
			String result = ConnectUtils.getResponse(url, accessToken, null, null, CLOUD_NAME.GMAIL,null);
			if(StringUtils.isNotEmpty(result)) {
				label = gson.fromJson(result, Label.class);
			}else {
				return null;
			}
		} catch (Exception e) {
			return null;
		} 
		EmailInfo emailInfo = new EmailInfo();
		emailInfo.setFolder(true);
		emailInfo.setId(label.getId());
		emailInfo.setMailFolder(label.getName());
		return emailInfo;
	}


	private String getValidAccessToken(VendorOAuthCredential credential) {
		if(credential.getLastRefreshed()==null || LocalDateTime.now().isAfter(credential.getLastRefreshed().plusHours(1))) {
			return gmailHelper.verifyAccessToken(credential, true).getAccessToken();
		}
		return credential.getAccessToken();
	}


	private String getValidAccessToken(VendorOAuthCredential credential,String emailId) {
		credential = credentialImpl.findById(credential.getId());
		if(credential!=null && (credential.getLastRefreshed()==null || (credential.getLastRefreshed()!=null && LocalDateTime.now().isAfter(credential.getLastRefreshed().plusHours(1))))) {
			try {
				return gmailHelper.getAccessTokenForUser(emailId,credential.getRefreshToken()).getAccessToken();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return credential.getAccessToken();
	}


	@Override
	public List<Clouds> getDeltaUsersList(ConnectFlags connectFlags) {
		List<Clouds> users = new ArrayList<>();
		MembersList membersList = null;
		String url = null;
		Clouds cloud = cloudsRepoImpl.findAdmin(connectFlags.getAdminMemberId(),connectFlags.getUserId());
		ConnectUtils.checkClouds(cloud);

		do {
			url = membersList == null
					? BASE_URL + "?customer=my_customer&viewType=admin_view&maxResults=" + USERS_FETCH_LIMIT
							: BASE_URL + "?customer=my_customer&viewType=admin_view" + MAXRESULTS + USERS_FETCH_LIMIT
							+ PAGE_TOKEN + membersList.getNextPageToken();

			String result = ConnectUtils.getResponse(url, connectFlags.getAccessToken(), null, cloud.getAdminEmailId(),CLOUD_NAME.GMAIL,cloud.getId());

			try {
				membersList = gson.fromJson(result, MembersList.class);
				if (membersList != null) {
					membersList.getUsers().forEach(user -> {
						try {
							Clouds saaSUser = new Clouds();
							if (!user.getPrimaryEmail().equals(connectFlags.getEmailId())) {
								saaSUser.setName(user.getName().getFullName());
								saaSUser.setMemberId(user.getId());
								saaSUser.setEmail(user.getPrimaryEmail());
								saaSUser.setActive(!user.getSuspended() && user.getIsMailboxSetup());
								if(Boolean.TRUE.equals(user.getSuspended())){
									saaSUser.setErrorDescription(user.getSuspensionReason());
								}
								if(Boolean.FALSE.equals(user.getIsMailboxSetup())) {
									saaSUser.setMailBoxStatus(ExceptionConstants.MAILBOXNOT_FOUND);
								}
								users.add(saaSUser);
							}
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
					});
				}

			} catch (Exception e) {
				log.error("Exception while fetching member details   ::" + ExceptionUtils.getStackTrace(e));
			}

		} while (membersList != null && StringUtils.isNotBlank(membersList.getNextPageToken()));
		return users;
	}

	@Override
	public List<EmailFlagsInfo> getDeltaChanges(EmailFlagsInfo emailFlagsInfo, String deltaChangeId)
			throws MailMigrationException {
		DeltaMails mailsList = null;
		long historyId = 0;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds adminCloud = cloudsRepoImpl.findOne(cloud.getAdminCloudId());
		ConnectUtils.checkClouds(adminCloud);
		Map<String,EmailFlagsInfo> emailFlags = new HashMap<>();
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String mailFolder = emailFlagsInfo.getFolder();
		List<String>dups = new ArrayList<>();
		if(StringUtils.isBlank(mailFolder)) {
			mailFolder = DEFAULT_MAIL_BOX;
		}
		if(emailFlagsInfo.isEvents()) {
			return getCalendarChanges(emailFlagsInfo, deltaChangeId);
		}
		if(null!=deltaChangeId && deltaChangeId.equals("0")) {
			emailFlagsInfo.setCopy(true);
			return getListOfMails(emailFlagsInfo);
		}
		Map<String,String>members = getMemberDetails(cloud.getUserId(), cloud.getAdminCloudId());
		Map<String,Long> threadOrder = new HashMap<>();
		do {
			try {
				String url = String.format(DELTA_CHANGES, cloud.getMemberId(),mailFolder,deltaChangeId);
				if(mailsList!=null && mailsList.getNextPageToken()!=null) {
					url =url+PAGE_TOKEN+ mailsList.getNextPageToken();
				}
				String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
				if(StringUtils.isNotBlank(result)) {
					mailsList = gson.fromJson(result, DeltaMails.class);
				}
				historyId = Integer.parseInt(mailsList.getHistoryId());
				if(ObjectUtils.isNotEmpty(mailsList) &&mailsList.getHistory()!=null && !mailsList.getHistory().isEmpty()) {
					emailFlagsInfo.setNextPageToken(mailsList.getNextPageToken());
					boolean customFolder = false;
					String cFolder = null;
					// added for custom folders in destination need to find with the label name instead of labelId
					if(MappingUtils.isCustomFolder(mailFolder)) {
						EmailFlagsInfo _info = new EmailFlagsInfo();
						_info.setFolder(mailFolder);
						_info.setCloudId(emailFlagsInfo.getCloudId());
						EmailInfo eInfo = getLabel(_info);
						cFolder = eInfo.getMailFolder();
						customFolder = true;
					}
					for(History message : mailsList.getHistory()) {
						if(message.getMessagesAdded()!=null && !message.getMessagesAdded().isEmpty()) {
							for(MessagesAdded messagesAdded : message.getMessagesAdded()) {
								if(messagesAdded.getMessage()!=null) {
									getChanges(messagesAdded, historyId, mailFolder, emailFlags, acceeToken, cloud,
											emailFlagsInfo,false,customFolder,cFolder,threadOrder,members,dups);
								}
							}
						}
						if(message.getMessagesDeleted()!=null && !message.getMessagesDeleted().isEmpty()){
							for(MessagesAdded messagesAdded : message.getMessagesDeleted()) {
								if(messagesAdded.getMessage()!=null) {
									getChanges(messagesAdded, historyId, mailFolder, emailFlags, acceeToken, cloud, emailFlagsInfo
											,true,customFolder,cFolder,threadOrder,members,dups);
								}

							}
						}
						if(message.getLabelsAdded()!=null && !message.getLabelsAdded().isEmpty()) {
							for(MessagesAdded messagesAdded : message.getLabelsAdded()) {
								if(messagesAdded.getMessage()!=null) {
									getChanges(messagesAdded, historyId, mailFolder, emailFlags, acceeToken, cloud, emailFlagsInfo,
											true,customFolder,cFolder,threadOrder,members,dups);
								}

							}
						}
						if(message.getLabelsRemoved()!=null && !message.getLabelsRemoved().isEmpty()) {
							for(MessagesAdded messagesAdded : message.getLabelsRemoved()) {
								if(messagesAdded.getMessage()!=null) {
									getChanges(messagesAdded, historyId, mailFolder, emailFlags, acceeToken, cloud, emailFlagsInfo,true,
											customFolder,cFolder,threadOrder,members,dups);
								}

							}
						}
					}
				}
			} catch (HttpClientErrorException e) {
				throw new MailMigrationException(ExceptionUtils.getStackTrace(e));
			} 
		}while(mailsList!=null && mailsList.getNextPageToken()!=null);
		emailFlagsInfo.setParentFolderId(""+historyId);
		threadOrder.clear();
		return new ArrayList<>(emailFlags.values());
	}

	/**
	 * Moving mails to From Folder to another folder
	 */
	@Override
	public EmailInfo moveEmails(EmailFlagsInfo emailFlagsInfo, EmailInfo emailInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		MailValue mailValue = null;
		String mailFolder = emailFlagsInfo.getFolder();
		if(StringUtils.isBlank(mailFolder)) {
			mailFolder = DEFAULT_MAIL_BOX;
		}
		String url = String.format(MOVE_MESSAGES,cloud.getMemberId(),emailInfo.getId());
		String accessToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		int count = 0;
		do {
			count+=1;
			try {
				if(StringUtils.isBlank(accessToken)) {
					return null; 
				}
				JSONObject body = new JSONObject();
				JSONArray rLables = new JSONArray();
				if(!emailInfo.getMailFolder().equalsIgnoreCase(emailFlagsInfo.getFolder())) {
					rLables.put(emailInfo.getMailFolder());
				}
				JSONArray aLables = new JSONArray();
				aLables.put(emailFlagsInfo.getFolder());
				aLables = emailFlagsInfo.isRead()?aLables.put("READ"):aLables.put("UNREAD");
				if(emailFlagsInfo.getImportance()!=null) {
					aLables.put("STARRED");
				}

				body.put("addLabelIds", aLables);
				body.put("removeLabelIds", rLables);
				String result = ConnectUtils.postResponse(url, accessToken, body.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
				if(result!=null) {
					mailValue = gson.fromJson(result, MailValue.class);
				}
				if(ObjectUtils.isNotEmpty(mailValue)) {
					EmailInfo info = new EmailInfo();
					info.setId(mailValue.getId());
					return info;
				}
			} catch (HttpClientErrorException e) {
				if(e.getMessage().contains("Invalid label")) {
					EmailFlagsInfo info = new EmailFlagsInfo();
					info.setCloudId(emailFlagsInfo.getCloudId());
					info.setFolder(emailFlagsInfo.getFolder());
					EmailInfo mailfolder = getLabel(info);
					if(mailfolder!=null) {
						emailFlagsInfo.setFolder(mailfolder.getId());
					}
				}else {
					throw e;
				}
			}
		}while(count<2);
		return emailInfo;


	}

	@Override
	public List<String> getDomains(ConnectFlags connectFlags) {
		List<Clouds> users = new ArrayList<>();
		String url = "https://admin.googleapis.com/admin/directory/v1/customer/my_customer/domains";
		DomainsList domainsList = null; 
		List<String> values = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findAdmin(connectFlags.getAdminMemberId(),connectFlags.getUserId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());

		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			domainsList = gson.fromJson(result, DomainsList.class); 
		}
		for(Domain domain : domainsList.getDomains()) {
			values.add(domain.getDomainName());
		}
		return values;
	}

	@Override
	public EmailInfo updateMetadata(EmailFlagsInfo emailFlagsInfo) throws Exception {
		return null;
	}

	@Override
	public List<CalenderInfo> getCalendarEvents(CalenderFlags emailFlagsInfo) {
		List<CalenderInfo> mailFolders = new ArrayList<>();
		EventsList emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());

		String url = String.format(GET_CALENDAR_EVENTS, emailFlagsInfo.getId());
		if(emailFlagsInfo.getNextPageToken()!=null) {
			url = url+"?pageToken="+emailFlagsInfo.getNextPageToken();
		}
		boolean primary = false;
		if(emailFlagsInfo.getId().equals(cloud.getEmail())) {
			primary = true;
		}
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		boolean checkDatainNext = false;
		if(!StringUtils.isBlank(result)) { 
			try { 
				emailFolders = gson.fromJson(result, EventsList.class); 
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		List<String>dups = new ArrayList<>();
		while(true) {
			if(emailFolders!=null ) { 
				emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
				emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
				if((emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) || checkDatainNext) {
					//Bug # added for first response came empty events but next pagetoken events are there soo we are skipping in connectorLoadTask
					url = String.format(GET_CALENDAR_EVENTS_page, emailFlagsInfo.getId(),emailFolders.getNextPageToken());
					result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
					emailFolders = gson.fromJson(result, EventsList.class); 
					emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
					emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
					//#Bug ended
				}
				log.info("--Total Events Fetched--"+cloud.getId()+"--"+emailFolders.getItems().size());
				for(EventItem calendar : emailFolders.getItems()) {
					if(!primary  || calendar.getId().split("_").length>1 || dups.contains(calendar.getId().split("_")[0])) {
						continue;
					}
					dups.add(calendar.getId().split("_")[0]);
					String _url = String.format(GET_CALENDAR_EVENTS, emailFlagsInfo.getId());
					_url = _url+"/"+calendar.getId();
					String _result = ConnectUtils.getResponse(_url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
					EventItem _calendar = gson.fromJson(_result, EventItem.class); 
					if(_calendar==null ||(_calendar!=null && dups.contains(_calendar.getICalUID()))) {
						continue;
					}
					dups.add(_calendar.getICalUID());
					CalenderInfo  info = gmailHelper.createCalendarInfo(_calendar, cloud, emailFolders);
					if(info!=null) {
						if(calendar.getOrganizer()!=null) {
							info.setExternalOrganizer(!MappingUtils.checkOrganizerExists(calendar.getOrganizer().getEmail(), cloud.getUserId(), cloud.getAdminCloudId(), cloudsRepoImpl));
						}
						mailFolders.add(info);
					}
				}
				if(mailFolders.isEmpty() && !emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) {
					checkDatainNext = true;	
				}else {
					break;
				}

			}
			if(!mailFolders.isEmpty() && emailFolders.getNextPageToken()==null) {
				break;
			}
		}
		return mailFolders;
	}
	
	public List<CalenderInfo> getCalendarEventsByTime(CalenderFlags emailFlagsInfo) {
		
		List<CalenderInfo> mailFolders = new ArrayList<>();
		EventsList emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		emailFlagsInfo.setStartTime(URLEncoder.encode(TimeUtils.convertGoogleTimeFormatWithOffset(emailFlagsInfo.getStartTime(),cloud.getTimeZone()==null?"UTC":cloud.getTimeZone())));
		emailFlagsInfo.setEndTime(URLEncoder.encode(TimeUtils.convertGoogleTimeFormatWithOffset(emailFlagsInfo.getEndTime(),cloud.getTimeZone()==null?"UTC":cloud.getTimeZone())));
		String url = String.format(GET_CALENDAR_EVENTS_BY_TIME, emailFlagsInfo.getId(),emailFlagsInfo.getStartTime(),emailFlagsInfo.getEndTime());
		if(emailFlagsInfo.getNextPageToken()!=null) {
			url = url+"?pageToken="+emailFlagsInfo.getNextPageToken();
		}
		boolean primary = false;
		if(emailFlagsInfo.getId().equals(cloud.getEmail())) {
			primary = true;
		}
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		boolean checkDatainNext = false;
		if(!StringUtils.isBlank(result)) { 
			try { 
				emailFolders = gson.fromJson(result, EventsList.class); 
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		List<String>dups = new ArrayList<>();
		while(true) {
			if(emailFolders!=null ) { 
				emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
				emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
				if((emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) || checkDatainNext) {
					//Bug # added for first response came empty events but next pagetoken events are there soo we are skipping in connectorLoadTask
					url = String.format(GET_CALENDAR_EVENTS_page, emailFlagsInfo.getId(),emailFolders.getNextPageToken());
					result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
					emailFolders = gson.fromJson(result, EventsList.class); 
					emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
					emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
					//#Bug ended
				}
				for(EventItem calendar : emailFolders.getItems()) {
					if(!primary  || calendar.getId().split("_").length>1 || dups.contains(calendar.getId().split("_")[0]) ||(calendar.getOrganizer()!=null && calendar.getCreator()!=null && ( MappingUtils.checkOrganizerWithSourceEmail(calendar.getOrganizer().getEmail(),cloud.getUserId(),cloud.getAdminCloudId(),cloud.getEmail(),cloudsRepoImpl) ||  MappingUtils.checkOrganizerWithSourceEmail(calendar.getCreator().getEmail(),cloud.getUserId(),cloud.getAdminCloudId(),cloud.getEmail(),cloudsRepoImpl)))) {
						continue;
					}
					dups.add(calendar.getId().split("_")[0]);
					String _url = String.format(GET_CALENDAR_EVENTS, emailFlagsInfo.getId());
					_url = _url+"/"+calendar.getId();
					String _result = ConnectUtils.getResponse(_url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
					EventItem _calendar = gson.fromJson(_result, EventItem.class); 
					if(_calendar==null ||(_calendar!=null && dups.contains(_calendar.getICalUID()))) {
						continue;
					}
					dups.add(_calendar.getICalUID());
					CalenderInfo  info = gmailHelper.createCalendarInfo(_calendar, cloud, emailFolders);
					if(info!=null) {
						if(calendar.getOrganizer()!=null) {
							info.setExternalOrganizer(!MappingUtils.checkOrganizerExists(calendar.getOrganizer().getEmail(), cloud.getUserId(), cloud.getAdminCloudId(), cloudsRepoImpl));
						}
						mailFolders.add(info);
					}
				}
				if(mailFolders.isEmpty() && !emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) {
					checkDatainNext = true;	
				}else {
					break;
				}

			}
			if(!mailFolders.isEmpty() && emailFolders.getNextPageToken()==null) {
				break;
			}
		}
		return mailFolders;
	}

	@Override
	public CalenderInfo createCalender(CalenderFlags calenderFlags) {
		Item item = null;	
		Clouds cloud = cloudsRepoImpl.findOne(calenderFlags.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = CREATE_CALENDAR;
		String input = createBodyForCalendar(calenderFlags);
		String result = ConnectUtils.postResponse(url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			item = gson.fromJson(result, Item.class);
		}
		if(item!=null) {
			CalenderInfo calenderInfo = new CalenderInfo();
			calenderInfo.setOrganizer(cloud.getEmail());
			calenderInfo.setSubject(item.getSummary());
			calenderInfo.setSourceId(item.getId());
			calenderInfo.setId(item.getId());
			calenderInfo.setCalender(true);
			boolean primary = false;
			try {
				if(item.getPrimary()!=null) {
					primary = true;
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
			calenderInfo.setPrimaryCalender(primary);
			return calenderInfo;
		}


		return null;
	}

	@Override
	public CalenderInfo createCalenderEvent(CalenderFlags calenderFlags) {
		EventItem eventItem = null;
		Clouds cloud = cloudsRepoImpl.findOne(calenderFlags.getCloudId());
		ConnectUtils.checkClouds(cloud);
		setTimeZone(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		List<AttachmentsData> attachIds = null;
		int conferenceId = calenderFlags.getOnlineMeetingUrl()!=null?1:0;
		String url = calenderFlags.isExternalOrg()? String.format(IMPORT_CALENDAR_EVENT,calenderFlags.getCalendar(),conferenceId):String.format(INSERT_CALENDAR_EVENT,calenderFlags.getCalendar(),conferenceId);
		if(calenderFlags.getAttachments()!=null && !calenderFlags.getAttachments().isEmpty()) {
			EmailFlagsInfo flagsInfo = new EmailFlagsInfo();
			flagsInfo.setCloudId(calenderFlags.getCloudId());
			flagsInfo.setAttachments(calenderFlags.getAttachments());
			flagsInfo.setFolder(calenderFlags.getCalendar());
			attachIds =  uploadFile(flagsInfo);
		}
		boolean isCloudmailPresentForImport = false;
		List<String>attendees = new ArrayList<>();
		attendees.addAll(calenderFlags.getAttendees());
		if(calenderFlags.isExternalOrg()){
			for(String attendee : attendees) {
				attendee = attendee.split(":")[0];
				if(attendee.equals(cloud.getEmail())) {
					isCloudmailPresentForImport = true;
					break;
				}
			}
			if(!isCloudmailPresentForImport) {
				attendees.add(cloud.getEmail()+":"+"needsAction");
			}
		}
		String input = gmailHelper.createBodyForEvent(calenderFlags,attachIds,attendees);
		String result = null;
		if(calenderFlags.getDestId()!=null) {
			// as we are impoting an event can't change so deleting event and creating again with the same details in delta// change in case found any API to update Private copy
			String _url ="https://www.googleapis.com/calendar/v3/calendars/"+calenderFlags.getCalendar()+"/events/"+calenderFlags.getDestId()+"?supportsAttachments=true";
			if(calenderFlags.isExternalOrg()) {
				result = ConnectUtils.deleteResponse(_url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			}
			result = ConnectUtils.putResponse(_url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		}else {
			result = ConnectUtils.postResponse(url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		}
		if(!StringUtils.isEmpty(result)) {
			eventItem = gson.fromJson(result, EventItem.class);
		}

		return gmailHelper.createCalendarInfo(eventItem, cloud, null);
	}

	@Override
	public List<CalenderInfo> getCalendars(CalenderFlags emailFlagsInfo) {
		List<CalenderInfo> mailFolders = new ArrayList<>();
		CalendarsList emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = GET_CALENDARS;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(!StringUtils.isBlank(result)) { 
			try { 
				emailFolders = gson.fromJson(result, CalendarsList.class); 
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
			if(emailFolders!=null ) { 
				for(Item calendar : emailFolders.getItems()) { 
					if(calendar.getAccessRole()!=null && calendar.getAccessRole().equalsIgnoreCase("owner")) {
						CalenderInfo calenderInfo = new CalenderInfo();
						calenderInfo.setOrganizer(cloud.getEmail());
						calenderInfo.setSubject(calendar.getSummary());
						calenderInfo.setSourceId(calendar.getId());
						calenderInfo.setId(calendar.getId());
						calenderInfo.setCalender(true);
						boolean primary = false;
						try {
							if(calendar.getPrimary()!=null) {
								primary = true;
							}
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
						calenderInfo.setPrimaryCalender(primary);
						mailFolders.add(calenderInfo);
					}
				}
			} 
			return mailFolders;
		}
		return Collections.emptyList();
	}

	@Override
	public CalenderInfo getCalendar(CalenderFlags emailFlagsInfo) {
		Item calendar = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String mailFolder = emailFlagsInfo.getCalendar();
		String url = String.format(GET_CALENDAR, emailFlagsInfo.getCalendar());
		try { 
			String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if(!StringUtils.isBlank(result)) { 
				calendar = gson.fromJson(result, Item.class); 
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return null;
		}
		if(calendar!=null ) { 
			if(calendar.getAccessRole()!=null && calendar.getAccessRole().equalsIgnoreCase("owner") && calendar.getSummary().equalsIgnoreCase(mailFolder)) {
				CalenderInfo calenderInfo = new CalenderInfo();
				calenderInfo.setOrganizer(cloud.getEmail());
				calenderInfo.setSubject(calendar.getSummary());
				calenderInfo.setSourceId(calendar.getId());
				calenderInfo.setId(calendar.getId());
				calenderInfo.setPrimaryCalender(calendar.getPrimary());
				return calenderInfo;
			}
		}
		return null;
	}

	@Override
	public CalenderInfo updateCalendarMetadata(CalenderFlags emailFlagsInfo) throws Exception {
		return null;
	}

	private EmailFlagsInfo createFlagsFromMails(MailValue mailValue,String folder,boolean stopCalendarNotifications,Clouds cloud,Map<String,String>members) {
		return gmailHelper.createFlagsFromMails(mailValue, folder, stopCalendarNotifications, cloud,members);
	}

	@Override
	public List<String> addAttachment(EmailFlagsInfo emailFlagsInfo, boolean event) throws IOException {
		EventItem eventItem = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		if(cloud.getDriveId()==null) {
			cloud.setDriveId(getDriveDetails(acceeToken,cloud.getCredential().getId()).getRootFolderId());
			cloudsRepoImpl.save(cloud);
		}
		List<String> attachs = new ArrayList<>();
		List<AttachmentsData> ids = uploadFile(emailFlagsInfo);
		emailFlagsInfo.setAttachments(ids);
		String url = null;
		if(event) {
			url = String.format(UPDATE_CALENDAR_EVENT,emailFlagsInfo.getFolder(), emailFlagsInfo.getId());
		}
		String input = gmailHelper.createBodyForAttachment(emailFlagsInfo);
		String result = ConnectUtils.patchResponse(url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(!StringUtils.isEmpty(result)) {
			eventItem = gson.fromJson(result, EventItem.class);
		}
		if(eventItem!=null) {
			for(Attachment attach : eventItem.getAttachments()) {
				attachs.add(attach.getFileId()+":"+attach.getTitle()+":"+attach.getMimeType()+":"+attach.getFileUrl());
			}
		}
		return attachs;
	}


	public List<AttachmentsData> uploadFile(EmailFlagsInfo emailFlagsInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		if(cloud.getDriveId()==null) {
			cloud.setDriveId(getDriveDetails(acceeToken,cloud.getCredential().getId()).getRootFolderId());
			cloudsRepoImpl.save(cloud);
		}
		List<AttachmentsData>attachMents = new ArrayList<>();
		JSONObject input = new JSONObject();
		if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
			for(AttachmentsData data : emailFlagsInfo.getAttachments()) {
				String encodedData = data.getContentBytes();
				if(encodedData==null) {
					continue;
				}
				if(data.getSize()>Const.ATTACHMENT_LIMIT) {
					try {
						attachMents.add(uploadLargeFile(emailFlagsInfo, data));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else {
					InputStream targetStream = null;
					//Replacing the Special characters except whiteSpaces in names for file uploading
					String ext = null;
					if(data.getName().split("\\.").length>1) {
						ext = data.getName().split("\\.")[1];
					}
					data.setName(Pattern.compile("[^a-zA-Z0-9 ]").matcher(data.getName().split("\\.")[0]).replaceAll("-"));
					if(ext!=null) {
						data.setName(data.getName()+"."+ext);
					}
					if(data.isEncoded()) {
						targetStream = new ByteArrayInputStream(Base64.decodeBase64(encodedData));
					}
					FileNameMap fileNameMap = URLConnection.getFileNameMap();
					String mime = fileNameMap.getContentTypeFor(data.getName());
					if(mime==null) {
						if(data.getName().endsWith(".docx")) {
							mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
						}
					}
					if(data.getContentType().equals("application/vnd.google-apps.document")) {
						String name = data.getName().lastIndexOf(".")>1? data.getName().substring(0,data.getName().lastIndexOf(".")):data.getName();
						data.setName(name==null?data.getName():name);
					}

					String url = String.format(CREATE_FILE,data.getName());
					FileMetadata fileMetadata = null;
					input.put("title", data.getName());
					if(data.getContentType() != null){
						input.put("mimeType", mime);
					}
					if(cloud.getDriveId()!=null) {
						JSONArray parentArray = new JSONArray();
						parentArray.put(cloud.getDriveId());
						input.put("parents", parentArray);
					}
					String result = ConnectUtils.postResponseFormValue(url, acceeToken, input.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,mime,data.getSize(),cloud.getId());
					log.info("===Location for Google File upload==CloudId=="+emailFlagsInfo.getCloudId()+"==url=="+result);
					if(StringUtils.isNotEmpty(result)) {
						try {
							result = ConnectUtils.putResponseStream(result, acceeToken, IOUtils.toByteArray(targetStream), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
						} catch (MailMigrationException | IOException e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
						fileMetadata = gson.fromJson(result, FileMetadata.class);
						if(ObjectUtils.isNotEmpty(fileMetadata)) {
							AttachmentsData attachmentsData = new AttachmentsData();
							attachmentsData.setId(fileMetadata.getId());
							attachmentsData.setContentType(fileMetadata.getMimeType());
							attachmentsData.setOdataType(fileMetadata.getAlternateLink());
							attachmentsData.setName(fileMetadata.getTitle());
							attachMents.add(attachmentsData);
						}
					}
				}
			}
		}
		return attachMents;
	}



	public AttachmentsData uploadLargeFile(EmailFlagsInfo emailFlagsInfo,AttachmentsData data) {
		log.info("==Entered for upload largeFile--"+data.getId()+"=="+data.getSize()+"=="+data.getName());
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		if(cloud.getDriveId()==null) {
			cloud.setDriveId(getDriveDetails(acceeToken,cloud.getCredential().getId()).getRootFolderId());
			cloudsRepoImpl.save(cloud);
		}
		File temp = null;
		JSONObject input = new JSONObject();
		if(data!=null) {
			try {
				if(data.isEncoded()) {
					temp = File.createTempFile("largeAtach"+data.getName(), ".temp");
					int offset = 0;
					int chunkSize = 8192;
					try (OutputStream outputStream = new FileOutputStream(temp)) {
						while (offset < data.getContentBytes().length()) {
							int endIndex = Math.min(offset + chunkSize, data.getContentBytes().length());
							String chunk = data.getContentBytes().substring(offset, endIndex);
							byte[] decodedChunk = Base64.decodeBase64(chunk);
							outputStream.write(decodedChunk);
							offset += chunkSize;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				FileNameMap fileNameMap = URLConnection.getFileNameMap();
				String mime = fileNameMap.getContentTypeFor(data.getName());
				if(mime==null) {
					if(data.getName()!=null && data.getName().endsWith(".docx")) {
						mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
					}
				}
				if(data.getContentType().equals("application/vnd.google-apps.document")) {
					String name = data.getName().substring(0,data.getName().lastIndexOf("."));
					data.setName(name==null?data.getName():name);
				}
				String url = String.format(CREATE_FILE,data.getName());
				FileMetadata fileMetadata = null;
				input.put("title", data.getName());
				if(data.getContentType() != null){
					input.put("mimeType", mime);
				}
				if(cloud.getDriveId()!=null) {
					JSONArray parentArray = new JSONArray();
					parentArray.put(cloud.getDriveId());
					input.put("parents", parentArray);
				}
				String result = ConnectUtils.postResponseFormValue(url, acceeToken, input.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,mime,data.getSize(),cloud.getId());
				log.info("===Location for Google File upload==CloudId=="+emailFlagsInfo.getCloudId()+"==url=="+result);
				UploadSession uploadSession = null;
				if(StringUtils.isNotEmpty(result)) {
					String ranges[] = {"0"};
					uploadSession = new UploadSession(cloud.getDriveId(), temp, result, ranges);
					while(!uploadSession.isComplete()) {
						byte[] bytesToUpload = uploadSession.getChunk();
						log.info("bytesToUpload length for attachments : "+bytesToUpload.length);
						String contentRange = String.format("bytes %d-%d/%d", uploadSession.getTotalUploaded(), uploadSession.getTotalUploaded() + bytesToUpload.length - 1, uploadSession.getFile().length());
						log.info("session upload length : "+String.format("bytes %d-%d/%d", uploadSession.getTotalUploaded(), uploadSession.getTotalUploaded() + bytesToUpload.length - 1, uploadSession.getFile().length())
						+" Total Uploaded : "+uploadSession.getTotalUploaded()+" session file length : "+uploadSession.getFile().length());
						if (uploadSession.getTotalUploaded() + bytesToUpload.length < uploadSession.getFile().length()) {
							result =  ConnectUtils.uploadSession(uploadSession.getUploadUrl(), bytesToUpload, contentRange, bytesToUpload.length,cloud.getId());
							if(StringUtils.isNotBlank(result)) {
								result = result.replace("bytes=", "");
								String _result = result;
								ranges[0]= _result;
								uploadSession.setRanges(ranges);
							}else{
								log.info("==fetched the blank result we can't upload the next ranges in=="+cloud.getId()+"==="+cloud.getEmail());
								break;
							}
						}else {
							result =  ConnectUtils.uploadSession(uploadSession.getUploadUrl(), uploadSession.getChunk(), contentRange, bytesToUpload.length,cloud.getId());
							uploadSession.setComplete(true);
							fileMetadata = gson.fromJson(result, FileMetadata.class);
							if(ObjectUtils.isNotEmpty(fileMetadata)) {
								AttachmentsData attachmentsData = new AttachmentsData();
								attachmentsData.setId(fileMetadata.getId());
								attachmentsData.setContentType(fileMetadata.getMimeType());
								attachmentsData.setOdataType(fileMetadata.getAlternateLink());
								attachmentsData.setName(fileMetadata.getTitle());
								return attachmentsData;
							}

						}
					}
				}
			}catch(Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}finally{
				if(temp!=null)
					try {
						Files.delete(temp.toPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		}
		return null;
	}

	@Override
	public boolean deleteEmails(EmailFlagsInfo emailFlagsInfo, boolean event) {

		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		setTimeZone(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/messages/"+emailFlagsInfo.getId();
		String result  = ConnectUtils.deleteResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,emailFlagsInfo.getCloudId());
		return result!=null;
	}


	public boolean deleteBatchEmails(Clouds cloud,EmailFlagsInfo emailFlagsInfo, boolean event,List<String>ids) {
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/messages/batchDelete";
		JSONObject jsonObject = new JSONObject();
		JSONArray array = new JSONArray(ids);
		jsonObject.put("ids", array);
		String result  = ConnectUtils.postResponse(url, acceeToken, jsonObject.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		return result!=null;
	}




	public List<EmailFlagsInfo> getCalendarChanges(EmailFlagsInfo emailFlagsInfo, String deltaChangeId)
			throws MailMigrationException {
		List<EmailFlagsInfo> mailFolders = new ArrayList<>();
		EventsList emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());

		String url = String.format(GET_CALENDAR_EVENTS, emailFlagsInfo.getFolder());
        url =  url+"?updatedMin="+deltaChangeId+"&maxResults=10";	
        do {
			if(emailFolders!=null && emailFolders.getNextPageToken()!=null) {
				url = url+"&pageToken="+emailFlagsInfo.getNextPageToken();
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if(!StringUtils.isBlank(result)) { 
				try { 
					emailFolders = gson.fromJson(result, EventsList.class); 
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
				if(emailFolders!=null ) { 
					emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
					emailFlagsInfo.setParentFolderId(emailFolders.getNextSyncToken());
					for(EventItem calendar : emailFolders.getItems()) { // for delta use the synctokenInthe response
//						String _url = String.format(GET_CALENDAR_EVENTS, emailFlagsInfo.getId());
//						_url = _url+"/"+_calendar.getId();
//						String _result = ConnectUtils.getResponse(_url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
//						EventItem calendar = gson.fromJson(_result, EventItem.class); 
						EmailFlagsInfo calenderInfo = new EmailFlagsInfo();
						calenderInfo.setSubject(calendar.getSummary());
						calenderInfo.setId(calendar.getId());
						calenderInfo.setBodyPreview(calendar.getSummary());
						if(calendar.getStatus()!=null && calendar.getStatus().equalsIgnoreCase("cancelled")) {
							calenderInfo.setDeleted(true);
							mailFolders.add(calenderInfo);
							continue;
						}
						calenderInfo.setFrom(calendar.getOrganizer().getEmail());
						calenderInfo.setStartTime(calendar.getStart().getDateTime()==null?calendar.getStart().getDate():calendar.getStart().getDateTime());
						calenderInfo.setTimeZone(calendar.getStart().getTimeZone()==null?emailFolders.getTimeZone() : calendar.getStart().getTimeZone());
						calenderInfo.setEndTime(calendar.getEnd().getDateTime()==null ?calendar.getEnd().getDate() : calendar.getEnd().getDateTime());
						calenderInfo.setEndTimeZone(calendar.getEnd().getTimeZone()==null ? calenderInfo.getTimeZone() : calendar.getEnd().getTimeZone());
						calenderInfo.setHtmlMessage(calendar.getDescription()==null?calendar.getSummary() : calendar.getDescription());
						calenderInfo.setHtmlMessage(StringEscapeUtils.unescapeHtml3(calenderInfo.getHtmlMessage()));
						calenderInfo.setColor(calendar.getColorId());
						if(calendar.getConferenceData()!=null && calendar.getConferenceData().getEntryPoints()!=null) {
							for(EntryPoint entryPoint : calendar.getConferenceData().getEntryPoints()) {
								if(entryPoint.getEntryPointType()!=null && entryPoint.getEntryPointType().equals("video")) {
									calenderInfo.setOnlineMeetingUrl(entryPoint.getUri());
								}
							}
						}
						if(calendar.getAttachments()!=null && !calendar.getAttachments().isEmpty()) {
							List<AttachmentsData> attachs = new ArrayList<>();
							calenderInfo.setHadAttachments(true);
							for(Attachment attach : calendar.getAttachments()) {
								AttachmentsData data = new AttachmentsData();
								data.setName(attach.getTitle());
								data.setId(attach.getFileId());
								data.setContentType(attach.getMimeType());
								data.setOdataType(attach.getFileUrl());
								attachs.add(data);
							}
							calenderInfo.setAttachments(attachs);
						}
						if(calendar.getRecurrence()!=null) {
							String recur = calendar.getRecurrence().get(0);
							Map<String, String> rruleMap = new HashMap<>();
							String[] rruleParts = recur.split(";");

							for (String rrulePart : rruleParts) {
								String[] keyValue = rrulePart.split("=");
								rruleMap.put(keyValue[0], keyValue[1]);
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
											LocalDateTime	untilLocalDateTime = LocalDateTime.parse(rruleMap.get("UNTIL"), java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'"));
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
								if(rruleMap.containsKey("COUNT")) {
									count = rruleMap.get("COUNT");
								}
								if(rruleMap.containsKey("INTERVAL")) {
									interval = rruleMap.get("INTERVAL");
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
								range = type+HASHTAG+startTime+HASHTAG+endTime+HASHTAG+wkst+HASHTAG+days+HASHTAG+count+HASHTAG+interval+HASHTAG;
								calenderInfo.setRange(range);
							}
						}
						if(emailFolders.getDefaultReminders()!=null && !emailFolders.getDefaultReminders().isEmpty()) {
							calenderInfo.setRemainderTime(emailFolders.getDefaultReminders().get(0).getMinutes());
						}
						if(calendar.getLocation()!=null) {
							calenderInfo.setLocation(Arrays.asList(calendar.getLocation()));
						}
						if(calendar.getReminders().getOverrides()!=null) {
							calenderInfo.setRemainders(calendar.getReminders().getOverrides().stream()
									.map(override -> override.getMethod() + Const.HASHTAG + override.getMinutes())
									.collect(Collectors.joining(",")));
						}
						if(calendar.getAttendees()!=null && !calendar.getAttendees().isEmpty()) {
							List<String>attendees = new ArrayList<>();
							for(Attendee attendee : calendar.getAttendees()) {
								attendees.add(attendee.getEmail()+":"+attendee.getResponseStatus()+":"+(attendee.isOptional()?"optional":"required")+":"+attendee.getComment());
							}
							calenderInfo.setTo(attendees);
						}
						
						mailFolders.add(calenderInfo);
					}
				} 
			}
		}while(emailFolders!=null && emailFolders.getNextPageToken()!=null);
		return mailFolders;
	}

	private String createBodyForCalendar(CalenderFlags calendarFlags) {
		JSONObject calendar = new JSONObject();
		calendar.put("summary", calendarFlags.getCalendar());
		return calendar.toString();
	}

	/**
	 * Mail Box Rules fetching from Source
	 * <br></br>
	 * See the Documentation for Reference in Google : <a href="https://developers.google.com/gmail/api/reference/rest/v1/users.settings.filters/list">Settings.Filter</a>
	 */
	@Override
	public List<EMailRules> getMailBoxRules(EmailFlagsInfo emailFlagsInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		MailBoxRules mailBoxRules = null;
		ConnectUtils.checkClouds(cloud);
		List<EMailRules> emailRules = new ArrayList<>();
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		do {
			String url = String.format(GET_MAIL_BOX_RULES, cloud.getMemberId());
			if(mailBoxRules!=null) {
				url =url+"&pageToken="+ mailBoxRules.getNextPageToken();
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if(StringUtils.isNotEmpty(result)) {
				mailBoxRules = gson.fromJson(result, MailBoxRules.class);
			}
			if(mailBoxRules!=null) {
				mailBoxRules.getFilter().forEach( filter ->{
					EMailRules ruls = gmailHelper.createEmailRules(filter);
					if(ruls!=null) {
						emailRules.add(ruls);
					}
				});
			}
		}while(mailBoxRules!=null && mailBoxRules.getNextPageToken()!=null);
		return emailRules;
	}

	/**
	 * Creating MailBox rule in GOOGLE
	 * For Documentation refer <a href="https://developers.google.com/gmail/api/reference/rest/v1/users.settings.filters/create">filters/create</a>
	 */
	@Override
	public EMailRules createMailBoxRule(EMailRules eMailRules, EmailFlagsInfo emailFlagsInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		Filter mailBoxRules = null;
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String input = gmailHelper.createBodyForRule(eMailRules);
		if(input == null) {
			return null;
		}
		String url = String.format(GET_MAIL_BOX_RULES, cloud.getMemberId());
		String result = ConnectUtils.postResponse(url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			mailBoxRules = gson.fromJson(result, Filter.class);
		}
		if(mailBoxRules!=null) {
			return  gmailHelper.createEmailRules(mailBoxRules);
		}
		return null;
	}

	private EmailFlagsInfo getChanges(MessagesAdded messagesAdded ,long historyId,String mailFolder,Map<String,EmailFlagsInfo> emailFlags,
			String acceeToken,Clouds cloud,EmailFlagsInfo emailFlagsInfo,boolean trash,boolean customFolder,String customName,
			Map<String,Long> threadOrder,Map<String,String>members,List<String>dups) {
		EmailFlagsInfo info = null;
		if(messagesAdded.getMessage()!=null) {
			if(dups.contains(messagesAdded.getMessage().getId())) {
				return null;
			}
			dups.add(messagesAdded.getMessage().getId());
			try {
				if(messagesAdded.getMessage().getLabelIds().contains(mailFolder)) {
					MailValue mailValue = null;
					String _url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/messages/"+messagesAdded.getMessage().getId()+"?includeSpamTrash="+trash;
					String result = ConnectUtils.getResponse(_url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
					if(StringUtils.isNotBlank(result)) {
						mailValue = gson.fromJson(result, MailValue.class);
					}
					if(ObjectUtils.isNotEmpty(mailValue)) {
						if(Long.valueOf(mailValue.getHistoryId())>historyId) {
							historyId = Long.valueOf(mailValue.getHistoryId());
						}
						info = gmailHelper.createFlagsFromMails(mailValue,mailFolder,emailFlagsInfo.isStopCalendarSource(),cloud,members);
						if(mailValue.getLabelIds().contains(mailFolder)) {
							for(String label : mailValue.getLabelIds())
							{
								if(MappingUtils.isCustomFolder(label)) {
									EmailFlagsInfo _info = new EmailFlagsInfo();
									_info.setFolder(label);
									_info.setCloudId(emailFlagsInfo.getCloudId());
									EmailInfo eInfo = getLabel(_info);
									if(eInfo!=null) {
										EmailFlagsInfo _mailFolder = createMailFolderInfo(eInfo);
										emailFlags.put(eInfo.getId(), _mailFolder);
										info.setFolder(_mailFolder.getName());
									}
								}
							}
						}
						if(customFolder) {
							info.setFolder(customName);
						}
						if(mailValue.getLabelIds().contains("TRASH")) {
							info.setFolder("TRASH");
							info.setDeleted(true);
						}else if(mailValue.getLabelIds().contains("SPAM")) {
							info.setFolder("SPAM");
						}
						if(info!=null && threadOrder!=null && threadOrder.containsKey(info.getThreadId())) {
							threadOrder.put(info.getThreadId(), threadOrder.get(info.getThreadId())+1);
						}else if(info!=null ) {
							threadOrder.put(info.getThreadId(), 0L);
						}
						if(info!=null) {
							info.setOrder(threadOrder.get(info.getThreadId()));
							emailFlags.put(mailValue.getId(), info);
						}
					}
				}
			} catch (HttpClientErrorException e) {
				log.error(e.getStatusText());
			} 
		}
		return info;
	}

	/**
	 * Get Drive Details for Uploading the attachments to the Gdrive
	 * Loading only once saving in Clouds so not required for everyTime
	 */
	public DriveAbout getDriveDetails(String acceeToken,String credId) {
		String url = "https://www.googleapis.com/drive/v2/about?fields=rootFolderId,quotaBytesByService";
		
		DriveAbout driveAbout = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, credId, CLOUD_NAME.GMAIL,credId);
		if(StringUtils.isNotEmpty(result)) {
			driveAbout = gson.fromJson(result, DriveAbout.class);
		}
		if(driveAbout!=null) {
			return driveAbout;
		}
		return null;
	}
	
	
	



	/**
	 * Get Drive Details for Uploading the attachments to the Gdrive
	 * Loading only once saving in Clouds so not required for everyTime
	 */
	private String getTimeZone(String cloudId) {
		String url = "https://www.googleapis.com/calendar/v3/users/me/settings";
		TokenResponse response = null;
		Clouds cloud = cloudsRepoImpl.findOne(cloudId);
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		response = gson.fromJson(result, TokenResponse.class); 
		if(response!=null && !response.getItems().isEmpty()) {
			return response.getItems().stream().distinct().filter(id->id.getId().equals("timezone")).findFirst().isPresent()?response.getItems().stream().distinct().filter(id->id.getId().equals("timezone")).findFirst().get().getValue():null;
		} 
		return null;
	}

	@Override
	public List<EmailUserSettings> getSettings(EmailFlagsInfo emailFlagsInfo) {
		List<EmailUserSettings> userSettings = new ArrayList<>();
		List<EmailUserSettings> settings = getSendAsSettings(emailFlagsInfo);
		Optional<EmailUserSettings> optForward =  getForwardSettings(emailFlagsInfo);
		EmailUserSettings forwardSettings = optForward.isPresent()?optForward.get():null;
		List<EmailUserSettings> delegateSettings =getDelegateSettings(emailFlagsInfo);
		EmailUserSettings autoFSettings = getAutoForwarding(emailFlagsInfo);
		EmailUserSettings imapSettings = getImapSettings(emailFlagsInfo);
		EmailUserSettings popSettings = getPopSettings(emailFlagsInfo);
		EmailUserSettings vocationSettings = getVocationSettings(emailFlagsInfo);
		userSettings.addAll(delegateSettings);
		if(forwardSettings!=null) {
			userSettings.add(forwardSettings);
		}
		userSettings.addAll(settings);
		userSettings.add(vocationSettings);
		userSettings.add(popSettings);
		userSettings.add(imapSettings);
		userSettings.add(autoFSettings);
		return userSettings;
	}

	public List<EmailUserSettings>getSendAsSettings(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/sendAs";
		SendAsSettings sendAsSettings = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		sendAsSettings = gson.fromJson(result, SendAsSettings.class);
		if(sendAsSettings==null) {
			return Collections.emptyList();
		}
		List<EmailUserSettings> elements = new ArrayList<>();
		if(ObjectUtils.isNotEmpty(sendAsSettings)) {
			sendAsSettings.getSendAs().forEach(sendAs->{
				EmailUserSettings emailSettings = gmailHelper.setEmailSettings(sendAs);
				elements.add(emailSettings);
			});
		}
		return elements;
	}

	public Optional<EmailUserSettings> getForwardSettings(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/forwardingAddresses";
		ForwardSettings forwardSettings = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		forwardSettings = gson.fromJson(result, ForwardSettings.class);
		if(forwardSettings==null) {
			return Optional.empty();
		}
		EmailUserSettings emailSettings = new EmailUserSettings();
		List<ForwardingAddresses> fAd = new ArrayList<>();
		forwardSettings.getForwardingAddresses().forEach(fAddress->{
			ForwardingAddresses forwardingAddresses = new ForwardingAddresses();
			forwardingAddresses.setForwardingEmail(fAddress.getForwardingEmail());
			forwardingAddresses.setVerificationStatus(fAddress.getVerificationStatus());
			fAd.add(forwardingAddresses);
		});
		emailSettings.setForwardingAddresses(fAd);
		return Optional.ofNullable(emailSettings);
	}


	public List<EmailUserSettings>getDelegateSettings(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/delegates";
		DelegateSettings forwardSettings = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		forwardSettings = gson.fromJson(result, DelegateSettings.class);
		List<EmailUserSettings> elements = new ArrayList<>();
		if(forwardSettings==null) {
			return Collections.emptyList();
		}
		forwardSettings.getDelegates().forEach(fAddress->{
			EmailUserSettings emailSettings = new EmailUserSettings();
			emailSettings.setDelegates(true);
			emailSettings.setEmail(fAddress.getDelegateEmail());
			emailSettings.setVerificationStatus(fAddress.getVerificationStatus());
			elements.add(emailSettings);
		});

		return elements;
	}



	public EmailUserSettings getAutoForwarding(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/autoForwarding";
		UserAutoForwarding userAutoForwarding = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userAutoForwarding = gson.fromJson(result, UserAutoForwarding.class);
		if(userAutoForwarding!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setAutoForwardSettings(userAutoForwarding);
			return emailUserSettings;
		}
		return null;
	}


	public EmailUserSettings getImapSettings(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/imap";
		UserImap userImap = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userImap = gson.fromJson(result, UserImap.class);
		if(userImap!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setImapSettings(userImap);
			return emailUserSettings;
		}
		return null;
	}


	public EmailUserSettings getPopSettings(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/pop";
		UserPopSetting userImap = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userImap = gson.fromJson(result, UserPopSetting.class);
		if(userImap!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setPopSettings(userImap);
			return emailUserSettings;
		}
		return null;
	}

	public EmailUserSettings getVocationSettings(EmailFlagsInfo emailFlagsInfo){
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/vacation";
		UserVocation userImap = null;
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userImap = gson.fromJson(result, UserVocation.class);
		if(userImap!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setVocationSetting(userImap);
			return emailUserSettings;
		}
		return null;
	}

	public Optional<EmailUserSettings> updateAutoForwarding(EmailUserSettings vocationSettings,EmailFlagsInfo emailFlagsInfo){
		if(vocationSettings ==null || vocationSettings.getAutoForwardSettings()==null) {
			return Optional.empty();
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/autoForwarding";
		UserAutoForwarding userAutoForwarding = null;
		JSONObject json = new JSONObject(vocationSettings.getAutoForwardSettings());
		String result = ConnectUtils.putResponse(url, acceeToken, json.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userAutoForwarding = gson.fromJson(result, UserAutoForwarding.class);
		if(userAutoForwarding!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setAutoForwardSettings(userAutoForwarding);
			return Optional.of(emailUserSettings);
		}
		return Optional.empty();
	}


	public Optional<EmailUserSettings> updateImapSettings(EmailUserSettings vocationSettings,EmailFlagsInfo emailFlagsInfo){
		if(vocationSettings ==null || vocationSettings.getImapSettings()==null) {
			return Optional.empty();
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/imap";
		UserImap userImap = null;
		JSONObject json = new JSONObject(vocationSettings.getImapSettings());
		String result = ConnectUtils.putResponse(url, acceeToken, json.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userImap = gson.fromJson(result, UserImap.class);
		if(userImap!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setImapSettings(userImap);
			return Optional.of( emailUserSettings);
		}
		return Optional.empty();
	}

	public Optional<EmailUserSettings> updateSendAsSettings(EmailUserSettings vocationSettings,EmailFlagsInfo emailFlagsInfo){
		if(vocationSettings ==null || !vocationSettings.isSendAs()) {
			return Optional.empty();
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		StringBuilder url = new StringBuilder("https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/sendAs");
		if(vocationSettings.isDefault()) {
			url = url.append("/"+cloud.getEmail());
			vocationSettings.setEmail(cloud.getEmail());
		}
		SendAs sendAs = null;
		String body = gmailHelper.createBodyForSendAsSettings(vocationSettings);
		String result = vocationSettings.isDefault() ?ConnectUtils.putResponse(url.toString(), acceeToken, body, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId()) : ConnectUtils.postResponse(url.toString(), acceeToken, body, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		sendAs = gson.fromJson(result, SendAs.class);
		if(sendAs!=null) {
			EmailUserSettings emailSettings = gmailHelper.setEmailSettings(sendAs);
			return Optional.of( emailSettings);
		}
		return Optional.empty();
	}

	public Optional<EmailUserSettings> updatePopSettings(EmailUserSettings vocationSettings,EmailFlagsInfo emailFlagsInfo){
		if(vocationSettings ==null || vocationSettings.getPopSettings()==null) {
			return Optional.empty();
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/pop";
		UserPopSetting userImap = null;
		JSONObject json = new JSONObject(vocationSettings.getPopSettings());
		String result = ConnectUtils.putResponse(url, acceeToken, json.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userImap = gson.fromJson(result, UserPopSetting.class);
		if(userImap!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setPopSettings(userImap);
			return Optional.of(emailUserSettings);
		}
		return Optional.empty();
	}

	public Optional<EmailUserSettings> updateVocationSettings(EmailUserSettings vocationSettings,EmailFlagsInfo emailFlagsInfo){
		if(vocationSettings ==null || vocationSettings.getVocationSetting()==null) {
			return Optional.empty();
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/vacation";
		UserVocation userImap = null;
		JSONObject json = new JSONObject(vocationSettings.getVocationSetting());
		String result = ConnectUtils.putResponse(url, acceeToken, json.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		userImap = gson.fromJson(result, UserVocation.class);
		if(userImap!=null) {
			EmailUserSettings emailUserSettings = new EmailUserSettings();
			emailUserSettings.setVocationSetting(userImap);
			return Optional.of(emailUserSettings);
		}
		return Optional.empty();
	}

	/**
	 * need to fix
	 */

	public Optional<EmailUserSettings> updateForwardingSettings(EmailUserSettings vocationSettings,EmailFlagsInfo emailFlagsInfo){
		if(vocationSettings ==null ||vocationSettings.getForwardingAddresses()==null ||vocationSettings.getForwardingAddresses().isEmpty()) {
			return Optional.empty();
		}
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String url = "https://gmail.googleapis.com/gmail/v1/users/"+cloud.getMemberId()+"/settings/forwardingAddresses";
		EmailUserSettings emailUserSettings = new EmailUserSettings();
		List<ForwardingAddresses>fAd = new ArrayList<>();
		for(ForwardingAddresses fAddress : vocationSettings.getForwardingAddresses()){
			JSONObject json = new JSONObject(fAddress);
			String result = null;
			ForwardingAddresses userImap = null;
			try {
				result = ConnectUtils.postResponse(url, acceeToken, json.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			} catch (HttpClientErrorException e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
			userImap = new ForwardingAddresses();
			userImap.setForwardingEmail(fAddress.getForwardingEmail());
			userImap.setVerificationStatus(fAddress.getVerificationStatus());
			fAd.add(userImap);
		};
		emailUserSettings.setForwardingAddresses(fAd);
		return Optional.of(emailUserSettings);
	}



	@Override
	public EmailUserSettings createUpdateSettings(EmailUserSettings emailUserSettings ,EmailFlagsInfo emailFlagsInfo) {
		EmailUserSettings userSettings = new EmailUserSettings();
		userSettings.setAutoForwardSettings(updateAutoForwarding(emailUserSettings, emailFlagsInfo).orElse(new EmailUserSettings()).getAutoForwardSettings());
		userSettings.setImapSettings(updateImapSettings(emailUserSettings, emailFlagsInfo).orElse(new EmailUserSettings()).getImapSettings());
		userSettings.setPopSettings(updatePopSettings(emailUserSettings, emailFlagsInfo).orElse(new EmailUserSettings()).getPopSettings());
		userSettings.setVocationSetting(updateVocationSettings(emailUserSettings, emailFlagsInfo).orElse(new EmailUserSettings()).getVocationSetting());
		userSettings.setForwardingAddresses(updateForwardingSettings(emailUserSettings, emailFlagsInfo).orElse(new EmailUserSettings()).getForwardingAddresses());
		if(emailUserSettings.isSendAs()) {
			userSettings = updateSendAsSettings(emailUserSettings, emailFlagsInfo).orElse(new EmailUserSettings());
		}else if(emailUserSettings.isDelegates()) {
			// need to add delegate settings update
		}
		return userSettings;
	}

	private int getRateLimitCount(String key) {
		return getConfigurer().getRateLimits().getOrDefault(key, 10);
	}

	@Override
	public List<UserGroups> getGroupEmailDetails(String adminCloudId){
		GroupsList groupsList = null;
		String rootUrl = null;
		List<UserGroups> listOfGrups = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		rootUrl = String.format(GET_GROUPS_ADMIN);
		do {
			if(groupsList!=null && groupsList.getNextPageToken()!=null) {
				rootUrl = String.format(GET_GROUPS_ADMIN_PAGINATION,groupsList.getNextPageToken());
			}
			try {
				String result = ConnectUtils.getResponse(rootUrl, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
				if (StringUtils.isEmpty(result)) {
					return Collections.emptyList();
				}
				groupsList = gson.fromJson(result, GroupsList.class);
				if(groupsList!=null && groupsList.getGroups()!=null && !groupsList.getGroups().isEmpty()){
					groupsList.getGroups().forEach(group ->{
						listOfGrups.add(gmailHelper.convertGroupToGroupEmailDetails(group, null));
					});
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}while(groupsList!=null && groupsList.getNextPageToken()!=null);
		return listOfGrups;
	}
	
	@Override
	public UserGroups getSingleGroupEmailDetails(String adminCloudId,String email){
		Group groupsList = null;
		String rootUrl = null;
		UserGroups userGroups = null;
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		rootUrl = String.format(GET_SINGLE_GROUP_ADMIN,email);
		try {
			String result = ConnectUtils.getResponse(rootUrl, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if (StringUtils.isEmpty(result)) {
				return null;
			}
			groupsList = gson.fromJson(result, Group.class);
			if(groupsList!=null){
				return gmailHelper.convertGroupToGroupEmailDetails(groupsList, null);
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return userGroups;
	}


	@Override
	public UserGroups createGroup(String adminCloudId,String email,String description,String name,List<String>members){
		Group groupsList = null;
		String rootUrl = null;
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		try {
			rootUrl = String.format(CREATE_GROUP_ADMIN);
			JSONObject input = new JSONObject();
			input.put("email", email);
			input.put("name", name);
			input.put("description", description);
			String result = ConnectUtils.postResponse(rootUrl, acceeToken, input.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if (StringUtils.isEmpty(result)) {
				return null;
			}
			groupsList = gson.fromJson(result, Group.class);
			if(groupsList!=null){
				if(members!=null) {
					addMembersToGroup(adminCloudId, members, groupsList.getId());
				}
				return gmailHelper.convertGroupToGroupEmailDetails(groupsList,members);
			}
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	@Override
	public List<String> addMembersToGroup(String adminCloudId,List<String>members,String groupId) {
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		List<String> listOfMembers = new ArrayList<>();
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), adminCloudId);
		String url = String.format(ADD_MEMEBERS_GROUP_ADMIN, groupId);
		JSONObject input = new JSONObject();
		for(String member : members) {
			MembersList groupsList = null;
			input.put("id",mappedEmailDetails.get(member.split(Const.HASHTAG)[0]));
			String targetRole = "MEMBER";
			String role = member.split(Const.HASHTAG)[1];
			if("OWNER".equals(role)) {
				targetRole = "OWNER";
			}else if("COOWNER".equals(role) || "EDIT".equals(role)) {
				targetRole = "MANAGER";
			}
			input.put("role", targetRole);
			String result = ConnectUtils.postResponse(url, acceeToken, input.toString(), cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			if (StringUtils.isEmpty(result)) {
				groupsList = gson.fromJson(result, MembersList.class);
				if(groupsList!=null && groupsList.getMembers()!=null && !groupsList.getMembers().isEmpty()){
					groupsList.getMembers().forEach(membe->
						listOfMembers.add(membe.getEmail()+Const.HASHTAG+ membe.getRole())
					);
				}
			}
		}
		return listOfMembers;
	}

	@Override
	public List<String> getMembersFromGroup(String adminCloudId,String groupId){
		MembersList groupsList = null;
		String rootUrl = null;
		List<String> listOfMembers = new ArrayList<>();
		Clouds cloud = cloudsRepoImpl.findOne(adminCloudId);
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		try {
			do {
				rootUrl = String.format(GET_GROUP_MEMBERS,groupId);
				if(groupsList!=null && groupsList.getNextPageToken()!=null) {
					rootUrl = String.format(GET_GROUP_MEMBERS_PAGINATION,groupId,groupsList.getNextPageToken());
				}
				String result = ConnectUtils.getResponse(rootUrl, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
				if (StringUtils.isEmpty(result)) {
					return Collections.emptyList();
				}
				groupsList = gson.fromJson(result, MembersList.class);
				if(groupsList!=null && groupsList.getMembers()!=null && !groupsList.getMembers().isEmpty()){
					groupsList.getMembers().forEach(member->
						listOfMembers.add(member.getEmail()+Const.HASHTAG+member.getRole())
					);
				}
			}while(groupsList!=null && groupsList.getNextPageToken()!=null);
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return listOfMembers;
	}



	private Map<String,String>getMemberDetails(String userId,String adminCloudId){
		return gmailHelper.getMemberDetails(userId, adminCloudId);
	}

	public String createContactsInCloud(String cloudId, List<Contacts> contacts,List<String> listOfErrorContactsMails) throws ContactCreationException  {
		Clouds cloud = cloudsRepoImpl.findOne(cloudId);
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		if(contacts == null || contacts.isEmpty()){
			return null;
		}
		try{
			String url = "https://people.googleapis.com/v1/people:createContact";
			for (Contacts eachContact : contacts) {

				JSONObject contactsInput = new JSONObject();
				contactsInput = gmailHelper.createBodyForContacts(cloudId, eachContact);
				String input = contactsInput.toString();
				String result = ConnectUtils.postResponse(url, acceeToken, input, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());

				log.info("----- END WITH CONTACT CREATION FOR---"+ eachContact.getFirstName());
				if(result != null && !result.equals("200") && listOfErrorContactsMails != null){
					for (Emails eachEmail : eachContact.getEmailAddresses()) {
						listOfErrorContactsMails.add(eachEmail.getEmailAddress());
						//break;
					}
				}

			}
		}catch(Exception e){
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	
	@Override
	public List<Contacts> listContacts(ContactsFlagInfo flagInfo ) throws ContactCreationException {
		Clouds cloud = cloudsRepoImpl.findOne(flagInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		ContactInfo contactInfo = null;
		List<Contacts> contactsList = new ArrayList<>();
		do{
			String contactUrl=null;
			if(contactInfo!=null){
				contactUrl = contactInfo.getNextLink();
			}else{
				contactUrl ="https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses,addresses,ageRanges,biographies,birthdays,calendarUrls,clientData,coverPhotos,events,externalIds,genders,imClients,,interests,locales,locations,memberships,miscKeywords,nicknames,occupations,organizations,phoneNumbers,photos,relations,sipAddresses,skills,urls,userDefined";
			}
			String result = ConnectUtils.getResponse(contactUrl, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			contactInfo =  gson.fromJson(result, ContactInfo.class);
			if(contactInfo!=null){
				for(Connection eachConnection : contactInfo.getConnections()){
					Contacts contacts = gmailHelper.getContactsInfo(eachConnection);
					if(null!=contacts) {
						contactsList.add(contacts);
					}
				}
			}
		}while(contactInfo!=null && contactInfo.getNextPageToken()!=null &&
				contactInfo.getNextPageToken().length() > 0);
		return contactsList;
	}
	
	@Override
	public List<CalenderInfo> getEventInstances(CalenderFlags emailFlagsInfo) {
		List<CalenderInfo> mailFolders = new ArrayList<>();
		EventsList emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String lastId = null;
		String oldToken = null;
		long latestDate = 0;
		Map<Long,String>dates = new HashMap<>();
		if(emailFlagsInfo.getStartTime().split("T").length>1) {
			latestDate = TimeUtils.convertRecurenceTimeToLocalDateTime(emailFlagsInfo.getStartTime().replaceAll("[-:]", ""), "UTC").toEpochSecond(ZoneOffset.UTC);
		}else {
			latestDate = Long.valueOf(TimeUtils.convertRecurenceTimeToLocalDate(emailFlagsInfo.getStartTime().replaceAll("[-:]", "")).toString().replaceAll("[-:]", ""));
		}
		dates.put(latestDate, emailFlagsInfo.getStartTime().replaceAll("[-:]", ""));
		do {
			String url = String.format(GET_CALENDAR_EVENT_INSTANCE,emailFlagsInfo.getCalendar(), emailFlagsInfo.getId());
			url = url+"&updatedMin="+emailFlagsInfo.getStartTime();
			if(emailFlagsInfo.getNextPageToken()!=null) {
				url = url+"&pageToken="+emailFlagsInfo.getNextPageToken();
			}
			String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			boolean checkDatainNext = false;
			if(!StringUtils.isBlank(result)) { 
				try { 
					emailFolders = gson.fromJson(result, EventsList.class); 
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
			if(emailFolders!=null ) { 
				emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
				if(null!=oldToken && oldToken.equals(emailFlagsInfo.getNextPageToken())) {
					break;
				}
				oldToken = emailFolders.getNextPageToken();
				emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
				emailFlagsInfo.setSyncToken(emailFolders.getNextSyncToken());
				if((emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) || checkDatainNext) {
					emailFolders = gson.fromJson(result, EventsList.class); 
					emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
					emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
				}
				boolean first = false;
				for(EventItem calendar : emailFolders.getItems()) {
					
					CalenderInfo  info = gmailHelper.createCalendarInfo(calendar, cloud, emailFolders);
					if(info!=null) {
						if(!first) {
							first = true;
							info.setPicking(true);
						}
						 long nexDate = Long.valueOf(TimeUtils.convertRecurenceTimeToLocalDateTime(info.getId().split("_")[1], "UTC").toEpochSecond(ZoneOffset.UTC));
						if(nexDate>latestDate) {
							latestDate=nexDate;
							dates.put(latestDate, info.getId().split("_")[1]);
						}
						if(calendar.getOrganizer()!=null) {
							info.setExternalOrganizer(!MappingUtils.checkOrganizerExists(calendar.getOrganizer().getEmail(), cloud.getUserId(), cloud.getAdminCloudId(), cloudsRepoImpl));
						}
						mailFolders.add(info);
					}
				}
				if(mailFolders.isEmpty() && !emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) {
					checkDatainNext = true;	
				}
			}
		}while(emailFlagsInfo.getNextPageToken()!=null);
		// here convert long time to calendarTime
		lastId = emailFlagsInfo.getId()+"_"+dates.get(latestDate);
		if(lastId!=null) {
			String lastTime = lastId.split("_")[1];
			lastId = setNextDateID(emailFlagsInfo, lastId, lastTime);
			emailFlagsInfo.setId(lastId);
			CalenderInfo info = null;
			try {
				info = getSingleEvent(emailFlagsInfo);
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
			if(info!=null) {
				mailFolders.add(info);
				if(!info.isDeleted() && !info.getUid().equals(lastId) && info.getUid().contains("_R")) {
					emailFlagsInfo.setId(info.getUid());
				}
			}
			mailFolders.addAll(getREventInstances(emailFlagsInfo));

		}
		return mailFolders;
	}

	private String setNextDateID(CalenderFlags emailFlagsInfo, String lastId, String lastTime) {
		if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("WEEKLY")) {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).plusWeeks(1).toString();
		}else if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("MONTHLY")) {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).plusMonths(1).toString();
		}else if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("YEARLY")) {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).plusYears(1).toString();
		}else {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).plusDays(1).toString();
		}
		lastTime= lastTime.replace("-", "");
		lastId= lastId.replace(lastId.split("_")[1].split("T")[0], lastTime);
		return lastId;
	}
	
	private String setPreviousDateID(CalenderFlags emailFlagsInfo, String lastId) {
		String lastTime = lastId;
		if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("WEEKLY")) {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).minusWeeks(1).toString();
		}else if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("MONTHLY")) {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).minusMonths(1).toString();
		}else if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("YEARLY")) {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).minusYears(1).toString();
		}else {
			lastTime = 	TimeUtils.convertRecurenceTimeToLocalDate(lastTime.split("T")[0]).minusDays(1).toString();
		}
		return lastTime= lastTime.replace("-", "");
	}
	
	public List<CalenderInfo> getREventInstances(CalenderFlags emailFlagsInfo) {
		List<CalenderInfo> mailFolders = new ArrayList<>();
		EventsList emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		String oldToken = null;
		long lastOccurence = 0;
		if(null!=emailFlagsInfo.getId()) {
			lastOccurence = setRId(emailFlagsInfo, lastOccurence);
		}
		boolean hadData = false;
		do {
			try {
				String url = String.format(GET_CALENDAR_EVENT_INSTANCE,emailFlagsInfo.getCalendar(), emailFlagsInfo.getId());
				if(emailFlagsInfo.getNextPageToken()!=null) {
					url = url+"&pageToken="+emailFlagsInfo.getNextPageToken();
				}
				String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
				boolean checkDatainNext = false;
				if(!StringUtils.isBlank(result)) { 
					try { 
						emailFolders = gson.fromJson(result, EventsList.class); 
					} catch (Exception e) {
						log.error(ExceptionUtils.getStackTrace(e));
					}
				}
				if(emailFolders!=null ) { 
					emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
					if(null!=oldToken && oldToken.equals(emailFlagsInfo.getNextPageToken())) {
						break;
					}
					oldToken = emailFolders.getNextPageToken();
					emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
					emailFlagsInfo.setSyncToken(emailFolders.getNextSyncToken());
					if((emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) || checkDatainNext) {
						emailFolders = gson.fromJson(result, EventsList.class); 
						emailFlagsInfo.setNextPageToken(emailFolders.getNextPageToken());
						emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
					}
					for(EventItem calendar : emailFolders.getItems()) {
						hadData = true;
						CalenderInfo  info = gmailHelper.createCalendarInfo(calendar, cloud, emailFolders);
						if(info!=null) {
							if(Long.valueOf(info.getId().split("_")[1].split("T")[0])>lastOccurence) {
								lastOccurence = Long.valueOf(info.getId().split("_")[1].split("T")[0]);
							}
							if(calendar.getOrganizer()!=null) {
								info.setExternalOrganizer(!MappingUtils.checkOrganizerExists(calendar.getOrganizer().getEmail(), cloud.getUserId(), cloud.getAdminCloudId(), cloudsRepoImpl));
							}
							mailFolders.add(info);
						}
					}
					if(mailFolders.isEmpty() && !emailFolders.getItems().isEmpty() && emailFolders.getNextPageToken()!=null) {
						checkDatainNext = true;	
					}
				}
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			} 
		}while(emailFlagsInfo.getNextPageToken()!=null);
		if(!hadData) {
			lastOccurence = Long.valueOf(setPreviousDateID(emailFlagsInfo, ""+lastOccurence));
		}
		emailFlagsInfo.setLastOccurence(""+lastOccurence);
		return mailFolders;
	}

	private long setRId(CalenderFlags emailFlagsInfo, long lastOccurence) {
		String rId = emailFlagsInfo.getId().split("_")[1];
		if(!rId.startsWith("R")) {
			lastOccurence = Long.valueOf(rId.split("T")[0]);
			if(rId.endsWith("Z")) {
				rId = rId.substring(0, rId.length()-1);
			}
			rId = "R"+rId;
		}
		emailFlagsInfo.setId(emailFlagsInfo.getId().split("_")[0]+"_"+rId);
		return lastOccurence;
	}	
	
	
	public CalenderInfo getSingleEvent(CalenderFlags emailFlagsInfo) {
		EventItem emailFolders = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		String acceeToken = getValidAccessToken(cloud.getCredential(),cloud.getEmail());
		long lastOccurence = 0;
		String url = String.format(GET_CALENDAR_EVENTS,emailFlagsInfo.getCalendar());
		url = url+"/"+emailFlagsInfo.getId();
		String result = ConnectUtils.getResponse(url, acceeToken, null, cloud.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
		if(!StringUtils.isBlank(result)) { 
			try { 
				emailFolders = gson.fromJson(result, EventItem.class); 
			} catch (Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
		}
		if(emailFolders!=null ) { 
			emailFlagsInfo.setParentFolderId(emailFolders.getUpdated());
			CalenderInfo  info = gmailHelper.createCalendarInfo(emailFolders, cloud, null);
			if(info!=null) {
				if(!info.isDeleted() && Long.valueOf(info.getId().split("_")[1].split("T")[0])>lastOccurence) {
					lastOccurence = Long.valueOf(info.getId().split("_")[1].split("T")[0]);
				}
				if(emailFolders.getOrganizer()!=null) {
					info.setExternalOrganizer(!MappingUtils.checkOrganizerExists(emailFolders.getOrganizer().getEmail(), cloud.getUserId(), cloud.getAdminCloudId(), cloudsRepoImpl));
				}
				return info;
			}
		}
			
		emailFlagsInfo.setLastOccurence(""+lastOccurence);
		return null;
	}	
	
	
}