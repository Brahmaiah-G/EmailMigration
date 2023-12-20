package com.testing.mail.connectors.impl.helper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.testing.mail.connectors.google.data.ContactInfo;
import com.testing.mail.connectors.google.data.FileMetadata;
import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.connectors.microsoft.data.AttachmentsUploadSession;
import com.testing.mail.connectors.microsoft.data.Attendee;
import com.testing.mail.connectors.microsoft.data.BatchRequests;
import com.testing.mail.connectors.microsoft.data.CalenderValue;
import com.testing.mail.connectors.microsoft.data.CalenderViewValue;
import com.testing.mail.connectors.microsoft.data.CalenderViews;
import com.testing.mail.connectors.microsoft.data.ContactValue;
import com.testing.mail.connectors.microsoft.data.EmailAttachMentValue;
import com.testing.mail.connectors.microsoft.data.EmailList;
import com.testing.mail.connectors.microsoft.data.ForwardTo;
import com.testing.mail.connectors.microsoft.data.FromAddress;
import com.testing.mail.connectors.microsoft.data.MailValue;
import com.testing.mail.connectors.microsoft.data.RefreshTokenResult;
import com.testing.mail.connectors.microsoft.data.Response;
import com.testing.mail.connectors.microsoft.data.SentToAddress;
import com.testing.mail.connectors.microsoft.data.Value;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.DBConstants;
import com.testing.mail.contacts.dao.ContactsFlagInfo;
import com.testing.mail.contacts.entities.Contacts;
import com.testing.mail.contacts.entities.Emails;
import com.testing.mail.contacts.entities.PhoneNumbers;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.impl.AppMongoOpsManager;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.MemberDetails;
import com.testing.mail.repo.entities.OAuthKey;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.impl.CloudsRepoImpl;
import com.testing.mail.repo.impl.VendorOAuthCredentialImpl;
import com.testing.mail.utils.ConnectUtils;
import com.testing.mail.utils.EventRangeUtils;
import com.testing.mail.utils.HttpUtils;
import com.testing.mail.utils.TimeUtils;
import com.testing.mail.utils.UploadSession;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OutlookHelper {

	private Gson gson = new Gson() ;
	@Autowired
	CloudsRepoImpl cloudsRepoImpl;
	@Autowired 
	private VendorOAuthCredentialImpl vendorOAuthCredentialRepo;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	AppMongoOpsManager appMongoOpsManager;
	private ObjectMapper objMapper = new ObjectMapper();
	@Autowired
	ContactsHelper contactsHelper;


	private static final String baseURL = "https://graph.microsoft.com/v1.0/";
	private static final String CREATE_FILE_URL_ADMIN = "users/%s/drive/items/%s/:/%s:/upload.createSession";
	private static final String USER_DRIVE_DETAIL = "users/%s/drive/root";
	private final String USERS ="/users";
	private final String BATCH_OPERATION ="https://graph.microsoft.com/v1.0/$batch";
	private final String SEND_MAIL_THREAD =baseURL+USERS+"/%s/messages";
	private final String DEFAULT_MAILBOX = "inbox";
	private final String GET_CALENDER_EVENTS = "users/%s/calendars/%s/events";
	private final String ADD_ATTACHMENT =baseURL+USERS+"/%s/messages";




	public Map<String,String>TIMEZONE_MAPPINGS = TimeUtils.loadTimeZones();

	/**
	 *Setting the ranges for the recurrence events
	 */
	public EventRangeUtils setRange(CalenderFlags emailFlagsInfo) {
		EventRangeUtils eventRangeUtils = new EventRangeUtils();
		if(emailFlagsInfo.getRange()!=null) {
			List<String> ranges = Arrays.asList(emailFlagsInfo.getRange().split(Const.HASHTAG));
			eventRangeUtils.setType(ranges.get(0));
			eventRangeUtils.setStartDate(ranges.get(1));
			if(eventRangeUtils.getType().equalsIgnoreCase("noEnd") && ranges.get(4).matches(".*\\d+.*")) {
				String number = ranges.get(4).replaceAll("[^0-9]", "");
				if(StringUtils.isNotBlank(number)) {
					int num = Integer.parseInt(number);
					number = "first";
					if(num==2) {
						number = "second";
					}else if(num==3) {
						number = "three";
					}else if(num==4) {
						number = "fourth";
					}
					eventRangeUtils.setIndex(number);
					ranges.set(4, ranges.get(4).replace(""+num, ""));
				}
			}
			eventRangeUtils.setEndDate(ranges.get(2));
			eventRangeUtils.setWkst(ranges.get(3));
			eventRangeUtils.setDays(ranges.get(4));
			if(eventRangeUtils.getDays()!=null && !eventRangeUtils.getDays().equals("null") && eventRangeUtils.getDays().startsWith("[")) {
				String _days = eventRangeUtils.getDays();
				_days = _days.substring(1, _days.length()-1);
				eventRangeUtils.setDays(_days);
			}
			eventRangeUtils.setOccurences(ranges.get(5));
			if(ranges.size()>6) {
				eventRangeUtils.setInterval(ranges.get(6));
			}
		}
		return eventRangeUtils;
	}


	/** Input for creating MailBoxRule  <br></br>
	 * For Reference See Documentation of MS-Graph <a href= "https://learn.microsoft.com/en-us/graph/api/mailfolder-post-messagerules?view=graph-rest-1.0&tabs=http">Rule-Creation</a> 
	 * @param EmailRules -POJO for creating body for a MailBoxRule
	 * @return String - Json to String converted data
	 */
	public String createBodyForRule(EMailRules eMailRules) {
		JSONObject rule = new JSONObject();
		eMailRules.setDisplayName(createDisplayName(eMailRules));
		rule.put("displayName", eMailRules.getDisplayName()==null?"RuleCopiedFromGoogle":eMailRules.getDisplayName());
		rule.put("isEnabled", eMailRules.isEnabled());
		rule.put("sequence", 1);
		JSONObject condition = new JSONObject();
		if(eMailRules.getFromAddresses()!=null && !eMailRules.getFromAddresses().isEmpty()) {
			JSONArray from = new JSONArray();
			JSONArray to = new JSONArray();
			for(String str : eMailRules.getFromAddresses()) {
				JSONObject email = new JSONObject();
				JSONObject address = new JSONObject();
				address.put("address", str);
				email.put("emailAddress", address);
				from.put(email);
			}
			if(eMailRules.getSentToAddresses()!=null) {
				for(String str : eMailRules.getSentToAddresses()) {
					JSONObject email = new JSONObject();
					JSONObject address = new JSONObject();
					address.put("address", str);
					email.put("emailAddress", address);
					to.put(email);
				}
				condition.put("sentToAddresses", to);
			}
			condition.put("fromAddresses", from);
		}
		if(eMailRules.getQuery()!=null) {
			JSONArray bodyContains = new JSONArray();
			bodyContains.put(eMailRules.getQuery());
			condition.put("bodyContains", bodyContains);
		}
		if(null!=eMailRules.getSubjectContains()) {
			JSONArray bodyContains = new JSONArray();
			bodyContains.put(eMailRules.getSubjectContains());
			condition.put("subjectContains", bodyContains);
		}
		if(eMailRules.isNegotiation()) {
			JSONObject exceptions = new JSONObject();
			JSONArray bodyContains = new JSONArray();
			bodyContains.put(eMailRules.getNegatedQuery());
			exceptions.put("bodyContains", bodyContains);
			rule.put("exceptions", exceptions);
		}

		condition.put("hasAttachments", eMailRules.isAttachments());
		if(eMailRules.getMinimumSize()>0 || eMailRules.getMaximumSize()>0) {
			JSONObject withinSizeRange = new JSONObject();
			withinSizeRange.put("minimumSize",eMailRules.getMinimumSize()>=10000000?(1000000) : eMailRules.getMinimumSize());
			eMailRules.setMaximumSize(eMailRules.getMaximumSize()>=10000000 ?(1000000):eMailRules.getMaximumSize());
			if(eMailRules.getMaximumSize()<=0) {
				eMailRules.setMaximumSize(eMailRules.getMinimumSize()>=10000000?((eMailRules.getMinimumSize()/10)+10):eMailRules.getMinimumSize()+10);
			}
			withinSizeRange.put("maximumSize",eMailRules.getMaximumSize()<=0?(eMailRules.getMinimumSize()+10):eMailRules.getMaximumSize());
			condition.put("withinSizeRange", withinSizeRange);
		}
		rule.put("conditions", condition);

		JSONObject action = new JSONObject();
		action.put("delete", eMailRules.isDelete());
		if(eMailRules.getForwards()!=null && !eMailRules.getForwards().isEmpty()) {
			JSONArray from = new JSONArray();
			for(String str : eMailRules.getForwards()) {
				JSONObject email = new JSONObject();
				JSONObject address = new JSONObject();
				address.put("address", str);
				email.put("emailAddress", address);
				from.put(email);
			}
			action.put("forwardTo", from);
		}
		if(eMailRules.getMailFolder()!=null && !eMailRules.getMailFolder().isEmpty()) {
			action.put("moveToFolder", eMailRules.getMailFolder().get(0));
		}
		//action.put("stopProcessingRules", true);
		if(eMailRules.isMarkImportance()) {
			action.put("markImportance", "high");
		}
		action.put("markAsRead", eMailRules.isMarkAsRead());
		rule.put("actions", action);
		return rule.toString();
	}

	/**
	 * Body For calendar Event
	 * <p>Check the RecurenceRule clearly it will impact on event Creation in outlook</p>
	 * @return JSONObject.toString()
	 */
	public String createBodyForEvent(CalenderFlags emailFlagsInfo,String dstTimeZone,Clouds cloud) {

		if(emailFlagsInfo==null) {
			throw new MailMigrationException("EmailFlags found null while creating body");
		}
		if(dstTimeZone==null) {
			dstTimeZone = "UTC";
		}
		JSONObject body = new JSONObject();
		JSONObject message = new JSONObject();
		EventRangeUtils eventRangeUtils = setRange(emailFlagsInfo);
		message.put("subject", emailFlagsInfo.getSubject());
		message.put("bodyPreview", emailFlagsInfo.getBodyPreview());
		message.put("importance", emailFlagsInfo.getImportance());
		body.put("contentType", "HTML");
		body.put("content", emailFlagsInfo.getMessage());
		message.put("body", body);
		if(emailFlagsInfo.getAttendees()!=null && !emailFlagsInfo.getAttendees().isEmpty()) {
			JSONArray array = new JSONArray();
			for(String tos : emailFlagsInfo.getAttendees()) {
				if(emailFlagsInfo.getOrganizer()!=null && emailFlagsInfo.getOrganizer().equalsIgnoreCase(tos.split(":")[0])) {
					continue;
				}
				JSONObject emailAddress = new JSONObject();
				JSONObject _to = new JSONObject();
				JSONObject response = new JSONObject();
				_to.put("address", tos.split(":")[0]);
				emailAddress.put("emailAddress", _to);
				String _type = tos.split(":").length>2 ? tos.split(":")[2]:"required";
				emailAddress.put("type", _type);
				if(tos.split(":").length>1) {
					String type = createResponseType(tos);
					response.put("response", type);
					emailAddress.put("status", response);
				}
				array.put(emailAddress);
			}
			message.put("attendees", array);
		}
		JSONObject start = new JSONObject();
		if(emailFlagsInfo.getTimeZone()==null) {
			emailFlagsInfo.setTimeZone("UTC");
		}
		// : Check for timeZones diff the calendar will change based on timeZones if it varies #Done
		if(emailFlagsInfo.getStartTime()!=null) {
			String endDate = TimeUtils.covertTimeZones(dstTimeZone, emailFlagsInfo.getTimeZone(), emailFlagsInfo.getStartTime());
			start.put("dateTime", endDate);
			start.put("timeZone", emailFlagsInfo.getTimeZone());
			message.put("start", start);
		}
		JSONObject end = new JSONObject();
		if(emailFlagsInfo.getEndTime()!=null) {
			String endDate = TimeUtils.covertTimeZones(dstTimeZone, emailFlagsInfo.getEndTimeZone(),emailFlagsInfo.getEndTime());
			end.put("dateTime", endDate);
			end.put("timeZone", emailFlagsInfo.getEndTimeZone());
			message.put("end", end);
		}
		JSONArray categories = new JSONArray();
		categories.put(emailFlagsInfo.getColour());
		//message.put("categories", categories);

		JSONObject onlineMetting = new JSONObject();
		JSONArray locations = new JSONArray();
		if(emailFlagsInfo.getOnlineMeetingUrl()!=null) {
			boolean googleLink = false;
			String joinUrl = emailFlagsInfo.getOnlineMeetingUrl().split(Const.HASHTAG)[1];
			if(joinUrl.contains("meet.google.com")) {
				googleLink = true;
			}else {
				onlineMetting.put("joinUrl", emailFlagsInfo.getOnlineMeetingUrl().split(Const.HASHTAG)[1]);
				message.put("onlineMeeting", onlineMetting);
			}
			Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), cloud.getAdminMemberId());
			if(googleLink && mappedEmailDetails.containsKey(emailFlagsInfo.getOrganizer())) {
				googleLink = true;
			}else if(googleLink) {
				googleLink = false;
			}
			JSONObject location = new JSONObject();
			location.put("displayName", googleLink?"Microsoft Teams Meeting":emailFlagsInfo.getOnlineMeetingUrl().split(Const.HASHTAG)[1]);
			location.put("uniqueId",  googleLink?"Microsoft Teams Meeting":emailFlagsInfo.getOnlineMeetingUrl().split(Const.HASHTAG)[1]);
			location.put("locationType", "default");
			location.put("uniqueIdType", "private");
			if(googleLink) {
				message.put("isOnlineMeeting", true);
				message.put("onlineMeetingProvider", "teamsForBusiness");
			}
			locations.put(location);
			if(emailFlagsInfo.getLocation()==null || emailFlagsInfo.getLocation().isEmpty()) {
				message.put("locations", locations);
			}
		}
		//default days in the recurence events
		String days = "sunday,monday,tuesday,wednesday,thursday,friday,saturday";

		if(emailFlagsInfo.getRecurrenceType()!=null) {
			JSONObject recurrence = createRecurenceRule(emailFlagsInfo, dstTimeZone, eventRangeUtils, days);
			message.put("recurrence", recurrence);
		}
		if(emailFlagsInfo.getLocation()!=null && !emailFlagsInfo.getLocation().isEmpty()) {
			for(String locationValues : emailFlagsInfo.getLocation()) {
				JSONObject location = new JSONObject();
				location.put("displayName", locationValues.split(":").length>1?locationValues.split(":")[1]:locationValues);
				location.put("uniqueId", locationValues.split(":").length>1?locationValues.split(":")[0]:locationValues);
				location.put("locationType", "default");
				location.put("uniqueIdType", "private");
				locations.put(location);
			}
			message.put("locations", locations);
		}
		return message.toString();
	}
	
	
	
	
	
	public String createBodyForEventDecline(CalenderFlags emailFlagsInfo,String dstTimeZone,Clouds cloud) {
		JSONObject object = new JSONObject();
		object.put("sendResponse", false);
		//object.put("comment", "Declined");
		return object.toString();
	}

	public JSONObject createRecurenceRule(CalenderFlags emailFlagsInfo, String dstTimeZone,
			EventRangeUtils eventRangeUtils, String days) {
		JSONObject recurrence = new JSONObject();
		JSONObject pattern = new JSONObject();
		pattern.put("type", EventRangeUtils.getModifiedType(emailFlagsInfo.getRecurrenceType()));
		pattern.put("index", "first");
		//parsing for the numbered recurence events(Particular events)
		LocalDate date = LocalDate.parse((emailFlagsInfo.getTimeZone().equals(dstTimeZone)?eventRangeUtils.getEndDate():TimeUtils.covertTimeZones(dstTimeZone, emailFlagsInfo.getTimeZone(), emailFlagsInfo.getEndTime())).split("T")[0]);
		int week = date.getDayOfMonth()/7;
		int _week = date.getDayOfMonth()%7;
		if(_week>0) {
			week = week+1;
		}
		String number = setNumber(week);
		eventRangeUtils.setIndex(number);
		boolean absolute = false;
		if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("MONTHLY") || emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("absoluteMonthly") || emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("relativeMonthly")) {
			if(eventRangeUtils.getDays()==null || eventRangeUtils.getDays().equalsIgnoreCase("null")) {
				pattern.put("type", "absoluteMonthly");
				pattern.put("dayOfMonth", date.getDayOfMonth());
				absolute = true;
			}
			pattern.put("index", eventRangeUtils.getIndex());
			eventRangeUtils.setDays(date.getDayOfWeek().name().toLowerCase());
			pattern = eventRangeUtils.getInterval()!=null && !eventRangeUtils.getInterval().contentEquals("null") ?pattern.put("interval", Integer.parseInt(eventRangeUtils.getInterval())):pattern.put("interval", 1);
		}else if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("YEARLY") || emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("absoluteYearly") || emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("relativeYearly")) {
			int month = 1;
			try {
				pattern.put("index", eventRangeUtils.getIndex());
				month = date.getMonthValue();
				if(eventRangeUtils.getDays()==null || eventRangeUtils.getDays().equalsIgnoreCase("null")) {
					pattern.put("type", "absoluteYearly");
					pattern.put("dayOfMonth", date.getDayOfMonth());
					absolute = true;
				}
				eventRangeUtils.setDays(date.getDayOfWeek().name().toLowerCase());
				eventRangeUtils.setStartDate(date.toString());
			}catch(Exception e) {
				log.error(ExceptionUtils.getStackTrace(e));
			}
			pattern.put("month", month);
		}else if(emailFlagsInfo.getRecurrenceType().equalsIgnoreCase("weekly")) {

			String wkst = "sunday";
			if(eventRangeUtils!=null) {
				if(eventRangeUtils.getWkst()!=null && !eventRangeUtils.getWkst().equals("null")) {
					wkst = eventRangeUtils.getWkst();
				}
			}
			pattern.put("firstDayOfWeek", wkst);
		}

		pattern = eventRangeUtils.getInterval()!=null && !eventRangeUtils.getInterval().contentEquals("null") ?pattern.put("interval", Integer.parseInt(eventRangeUtils.getInterval())):pattern.put("interval", 1);

		JSONObject range = new JSONObject();
		if((eventRangeUtils.getDays()!=null && eventRangeUtils.getDays().equalsIgnoreCase("null")) ||StringUtils.isBlank(eventRangeUtils.getDays())) {
			eventRangeUtils.setDays(days);
		}
		if(!absolute&& StringUtils.isNotBlank(eventRangeUtils.getDays()) && !eventRangeUtils.getDays().equals("null")) {
			JSONArray jsonArray = new JSONArray();

			List<String> daysList = Arrays.asList(eventRangeUtils.getDays().split(","));
			daysList.forEach(day->
			jsonArray.put(day.trim())
					);
			pattern.put("daysOfWeek", jsonArray);
		}
		if(eventRangeUtils.getWkst()!=null&& !eventRangeUtils.getWkst().equals("null")) {
			pattern.put("firstDayOfWeek", eventRangeUtils.getWkst());
		}
		if(eventRangeUtils!=null) {
			range.put("type", eventRangeUtils.getType());
			if(eventRangeUtils!=null && eventRangeUtils.getOccurences()!=null && !eventRangeUtils.getOccurences().equals("null") && !(eventRangeUtils.getOccurences().isEmpty() || eventRangeUtils.getOccurences().equals("0")) ) {
				range.put("numberOfOccurrences", eventRangeUtils.getOccurences());
				range.put("type", "numbered");
			}//#IMPORTANT here need to check as the  timeZone are changing and format is also changing
			//#DONE issue with outlook to outlook as we are checking for UTC only so that end time will be different for range and normal one need to check clearly
			String eventDate = emailFlagsInfo.getTimeZone().equals(dstTimeZone)?eventRangeUtils.getStartDate():TimeUtils.covertTimeZones(dstTimeZone, emailFlagsInfo.getTimeZone(), emailFlagsInfo.getStartTime());
			range.put("startDate", eventDate.split("T")[0]);
			if(!eventRangeUtils.getType().equals("noEnd")) {
				//#DONE added this for end time UTC is not getting from source then issue will raise please check once and instead of UTC check like destination mailBoxSetting timeZone so we can perform better
				eventDate = eventRangeUtils.getEndDate()+"T"+(emailFlagsInfo.getEndTime().split("T").length>1 ?emailFlagsInfo.getEndTime().split("T")[1]:"");
				eventDate = emailFlagsInfo.getEndTimeZone().equals(dstTimeZone)?eventRangeUtils.getEndDate():TimeUtils.covertTimeZones(dstTimeZone, emailFlagsInfo.getEndTimeZone(), eventDate);
				range.put("endDate", eventDate.split("T")[0]);
			}
			range.put("recurrenceTimeZone", emailFlagsInfo.getTimeZone());
			recurrence.put("pattern", pattern);
			recurrence.put("range", range);
		}
		return recurrence;
	}

	public String setNumber(int week) {
		String number;
		switch (week) {
		case 2:
			number = "second";
			break;
		case 3:
			number = "third";
			break;
		case 4:
			number = "fourth";
			break;
		default:
			number = "first";
			break;
		}
		return number;
	}



	public CalenderInfo createInforViews(CalenderViewValue value,String sourceParent) {
		CalenderInfo calenderInfo = new CalenderInfo();
		calenderInfo.setOrganizer(value.getOrganizer().getEmailAddress().getAddress());
		calenderInfo.setAllDay(value.getIsAllDay());
		calenderInfo.setAttachMents(value.getHasAttachments());
		List<String>attendess = new ArrayList<>();
		for(Attendee attendee:value.getAttendees()) {
			attendess.add(attendee.getEmailAddress().getAddress()==null ?attendee.getEmailAddress().getName():attendee.getEmailAddress().getAddress()+":"+attendee.getStatus().getResponse()+":"+attendee.getType());
		}
		calenderInfo.setAttendees(attendess);
		calenderInfo.setCalender(false);
		calenderInfo.setId(value.getId());
		calenderInfo.setBodyPreview(value.getBodyPreview());
		if(value.getBody()!=null) {
			try {
				calenderInfo.setHtmlContent(value.getBody().getContentType().equals("html"));
				calenderInfo.setHtmlBodyContent(value.getBody().getContent());
				Document doc = Jsoup.parse(value.getBody().getContent());
				calenderInfo.setHtmlBodyContent(removeTeamsMeetingLink(doc));
			} catch (Exception e) {
				calenderInfo.setHtmlBodyContent(value.getBody().getContent());
			}
		}
		calenderInfo.setCalenderCreatedTime(value.getCreatedDateTime());
		calenderInfo.setCalenderModifiedTime(value.getLastModifiedDateTime());
		calenderInfo.setStartTime(value.getStart().getDateTime());
		calenderInfo.setEndTime(value.getEnd().getDateTime());
		calenderInfo.setSubject(value.getSubject());
		calenderInfo.setUid(value.getUid());
		calenderInfo.setICalUId(value.getICalUId());
		calenderInfo.setType(value.getType());
		calenderInfo.setSourceParent(sourceParent);
		calenderInfo.setOnlineMeeting(value.getIsOnlineMeeting());
		calenderInfo.setOriginalFrom(value.getOrganizer().getEmailAddress().getAddress());
		calenderInfo.setFromName(value.getOrganizer().getEmailAddress().getName());
		calenderInfo.setOnlineMeetingUrl(value.getOnlineMeeting()!=null ? value.getOnlineMeeting().getJoinUrl():null);
		calenderInfo.setCategories(value.getCategories());
		calenderInfo.setTimeZone(TIMEZONE_MAPPINGS.containsKey(value.getOriginalStartTimeZone())?TIMEZONE_MAPPINGS.get(value.getOriginalStartTimeZone()):value.getOriginalStartTimeZone());
		calenderInfo.setEndTimeZone(TIMEZONE_MAPPINGS.containsKey(value.getOriginalEndTimeZone())?TIMEZONE_MAPPINGS.get(value.getOriginalEndTimeZone()):value.getOriginalEndTimeZone());
		calenderInfo.setDraft(value.getIsDraft());
		calenderInfo.setRemainderTime(value.getReminderMinutesBeforeStart());
		calenderInfo.setReminderOn(value.getIsReminderOn());
		if(value.getRecurrence()!=null) {
			calenderInfo.setRecurrenceType(value.getRecurrence().getPattern().getType());
			//Added in the form of type+HASHTAG+startTime+HASHTAG+endTime+HASHTAG+wkst+HASHTAG+days+HASHTAG+count+HASHTAG+interval
			calenderInfo.setRange(value.getRecurrence().getRange().getType()+Const.HASHTAG+value.getRecurrence().getRange().getStartDate()+Const.HASHTAG+value.getRecurrence().getRange().getEndDate()+Const.HASHTAG+value.getRecurrence().getPattern().getFirstDayOfWeek()+Const.HASHTAG+value.getRecurrence().getPattern().getDaysOfWeek()+Const.HASHTAG+(value.getRecurrence().getRange().getNumberOfOccurrences()==0?value.getRecurrence().getPattern().getIndex():value.getRecurrence().getRange().getNumberOfOccurrences())+Const.HASHTAG+value.getRecurrence().getPattern().getInterval());
		}
		if(value.getLocations()!=null && !value.getLocations().isEmpty()) {
			List<String>locations = new ArrayList<>();
			value.getLocations().forEach(location->
			locations.add(location.getUniqueId()+":"+location.getDisplayName()));
			calenderInfo.setLocation(locations);
		}
		return calenderInfo;
	}







	public CalenderInfo createEmailInfoForCalender(CalenderValue value) {
		CalenderInfo calenderInfo = new CalenderInfo();
		calenderInfo.setOrganizer(value.getOwner().getAddress());
		calenderInfo.setSubject(value.getName());
		calenderInfo.setSourceId(value.getId());
		calenderInfo.setId(value.getId());
		calenderInfo.setPrimaryCalender(value.getIsDefaultCalendar());
		calenderInfo.setCalender(true);
		calenderInfo.setReadOnly(!value.getCanEdit());

		return calenderInfo;
	}


	/**For updating the timestamp and from and names<p></p>
	 *
	 *<p></p>Refer Documentation for any queries: <i><a href = "https://learn.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxprops/f6ab1613-aefe-447d-a49c-18217230b148?redirectedfrom=MSDN">exchange_server_protocols </a></i>
	 */
	public String createBodyForTimeStampEvent(CalenderFlags emailFlagsInfo) {
		if(emailFlagsInfo.getFromName()==null) {
			emailFlagsInfo.setFromName(emailFlagsInfo.getOrganizer().split("@")[0]);
		}
		JSONObject timeStamp = new JSONObject();
		JSONArray singleValueProps = new JSONArray();
		JSONObject id = new JSONObject();
		id.put(DBConstants.ID, "Integer 0x0E07");
		id.put("value", "1");
		singleValueProps.put(id);
		JSONObject fromEmailAddress = new JSONObject();
		// for updating the organizer Email Address
		fromEmailAddress.put(DBConstants.ID, "String 0x5D02");
		fromEmailAddress.put("value", emailFlagsInfo.getOrganizer());
		singleValueProps.put(fromEmailAddress);
		// for updating the organizer Name
		if(emailFlagsInfo.getFromName()!=null) {
			JSONObject fromEmailName = new JSONObject();
			fromEmailName.put(DBConstants.ID, "String 0x0042");
			fromEmailName.put("value",  emailFlagsInfo.getFromName());//need to set the from name based on the fetching ones
			singleValueProps.put(fromEmailName);
		}
		JSONArray array = new JSONArray();
		if(emailFlagsInfo.getAttendees()!=null && !emailFlagsInfo.getAttendees().isEmpty()) {
			for(String tos : emailFlagsInfo.getAttendees()) {
				if(emailFlagsInfo.getOrganizer().equals(tos.split(":")[0])) {
					continue;
				}
				JSONObject emailAddress = new JSONObject();
				JSONObject _to = new JSONObject();
				JSONObject response = new JSONObject();
				if(tos.split(":").length>1) {
					String type = createResponseType(tos);
					response.put("response", type);
					emailAddress.put("status", response);
				}
				_to.put("address", tos.split(":")[0]);
				emailAddress.put("emailAddress", _to);
				array.put(emailAddress);
			}
			timeStamp.put("attendees", array);
		}
		timeStamp.put("singleValueExtendedProperties", singleValueProps);
		return timeStamp.toString();
	}

	public String createResponseType(String tos) {
		String type = tos.split(":")[1];
		if("tentative".equals(type) || "tentativelyAccepted".equals(type)) {
			type = "tentativelyAccepted";
		}else if("needsAction".equals(type) || "notResponded".equals(type)) {
			type = "notResponded";
		}else if("declined".equalsIgnoreCase(type)) {
			type = "declined";
		}else {
			type = "accepted";
		}
		return type;
	}


	public EmailFlagsInfo createFlags(CalenderViewValue value,String adminEmail) {
		if(value==null) {
			return null; 
		} 
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		if(value.getIsCancelled()!=null && value.getIsCancelled()==Boolean.TRUE) {
			emailFlagsInfo.setDeleted(true);
			emailFlagsInfo.setId(value.getId());
			return emailFlagsInfo;
		}
		try {
			if(value.getOrganizer()!=null) {
				emailFlagsInfo.setFrom(value.getOrganizer().getEmailAddress().getAddress());
				emailFlagsInfo.setFromName(value.getOrganizer().getEmailAddress().getName());
			}
			if(value.getAttendees()!=null && !value.getAttendees().isEmpty()) {
				List<String>to = new ArrayList<>(); 
				for(Attendee toRecipents :value.getAttendees()) {
					to.add(toRecipents.getEmailAddress().getAddress()==null ? toRecipents.getEmailAddress().getName() :toRecipents.getEmailAddress().getAddress()+":"+toRecipents.getStatus().getResponse()+":"+toRecipents.getType());
				}
				emailFlagsInfo.setTo(to);
			}
			if(value.getHasAttachments()!=null && value.getHasAttachments().booleanValue()) {
				emailFlagsInfo.setHadAttachments(true);
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
			if(value.getRecurrence()!=null) {
				emailFlagsInfo.setRecurrenceType(value.getRecurrence().getPattern().getType());
				//Added in the form of type+HASHTAG+startTime+HASHTAG+endTime+HASHTAG+wkst+HASHTAG+days+HASHTAG+count+HASHTAG+interval
				emailFlagsInfo.setRange(value.getRecurrence().getRange().getType()+Const.HASHTAG+value.getRecurrence().getRange().getStartDate()+Const.HASHTAG+value.getRecurrence().getRange().getEndDate()+Const.HASHTAG+value.getRecurrence().getPattern().getFirstDayOfWeek()+Const.HASHTAG+value.getRecurrence().getPattern().getDaysOfWeek()+Const.HASHTAG+value.getRecurrence().getRange().getNumberOfOccurrences()+Const.HASHTAG+value.getRecurrence().getPattern().getInterval());
			}
			if(value.getLocations()!=null && !value.getLocations().isEmpty()) {
				List<String>locations = new ArrayList<>();
				value.getLocations().forEach(location->
				locations.add(location.getUniqueId()+":"+location.getDisplayName())
						);
				emailFlagsInfo.setLocation(locations);
			}
			emailFlagsInfo.setStartTime(value.getStart().getDateTime());
			emailFlagsInfo.setEndTime(value.getEnd().getDateTime());
			emailFlagsInfo.setSubject(value.getSubject());
			emailFlagsInfo.setOnlineMeetingUrl(value.getOnlineMeeting()!=null ? value.getOnlineMeeting().getJoinUrl():null);
			//emailFlagsInfo.setCategories(value.getCategories());
			emailFlagsInfo.setTimeZone(TIMEZONE_MAPPINGS.containsKey(value.getOriginalStartTimeZone())?TIMEZONE_MAPPINGS.get(value.getOriginalStartTimeZone()):value.getOriginalStartTimeZone());
			emailFlagsInfo.setEndTimeZone(TIMEZONE_MAPPINGS.containsKey(value.getOriginalEndTimeZone())?TIMEZONE_MAPPINGS.get(value.getOriginalEndTimeZone()):value.getOriginalEndTimeZone());
			emailFlagsInfo.setDraft(value.getIsDraft());
			emailFlagsInfo.setRemainderTime(value.getReminderMinutesBeforeStart());
			emailFlagsInfo.setId(value.getId());
			emailFlagsInfo.setCreatedTime(value.getCreatedDateTime());
			emailFlagsInfo.setImportance(value.getImportance());
			emailFlagsInfo.setHadAttachments(value.getHasAttachments());
		} catch(Exception e) { 
			log.info(ExceptionUtils.getMessage(e)); 
		} 
		return emailFlagsInfo; 
	}

	public EMailRules createEmailRules(MailValue mailValue) {
		try {
			EMailRules eMailRules = new EMailRules();
			String action = null;
			if(mailValue.getActions().getMoveToFolder()!=null) {
				action = mailValue.getActions().getMoveToFolder();
			}else if(mailValue.getActions().getCopyToFolder()!=null) {
				action = mailValue.getActions().getCopyToFolder();
			}
			if(mailValue.getActions()!=null && mailValue.getActions().getForwardTo()!=null){
				List<String> emailAddress = new ArrayList<>();
				for(ForwardTo fwt : mailValue.getActions().getForwardTo()) {
					emailAddress.add(fwt.getEmailAddress().getAddress());
				}
				eMailRules.setForwards(emailAddress);
			}
			if(mailValue.getActions()!=null && mailValue.getActions().getRedirectTo()!=null){
				List<String> emailAddress = new ArrayList<>();
				for(ForwardTo fwt : mailValue.getActions().getRedirectTo()) {
					emailAddress.add(fwt.getEmailAddress().getAddress());
				}
				eMailRules.setRedirectTo(emailAddress);
			}
			eMailRules.setAction(action);
			eMailRules.setDelete(mailValue.getActions().isDelete());
			List<String> emailAddress = new ArrayList<>();
			if(mailValue.getConditions()!=null) {
				if(mailValue.getConditions().getFromAddresses()!=null) {
					for(FromAddress fromAddress : mailValue.getConditions().getFromAddresses()) {
						emailAddress.add(fromAddress.getEmailAddress().getAddress());
					}
					eMailRules.setFromAddresses(emailAddress);
				}else if(mailValue.getConditions().getSentToAddresses()!=null) {
					for(SentToAddress fromAddress : mailValue.getConditions().getSentToAddresses()) {
						emailAddress.add(fromAddress.getEmailAddress().getAddress());
					}
					eMailRules.setSentToAddresses(emailAddress);
				}
				eMailRules.setDisplayName(mailValue.getDisplayName());
				eMailRules.setId(mailValue.getId());
				eMailRules.setEnabled(mailValue.isEnabled());
			}
			return eMailRules;
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	public String createBodyForTimeStamp(EmailFlagsInfo emailFlagsInfo,String email) {
		//For updating the timestamp and from and names // refer for these https://learn.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxprops/f6ab1613-aefe-447d-a49c-18217230b148?redirectedfrom=MSDN
		JSONObject timeStamp = new JSONObject();
		JSONArray singleValueProps = new JSONArray();
		JSONObject id = new JSONObject();
		id.put(DBConstants.ID, "Integer 0x0E07");
		id.put("value", "1");
		singleValueProps.put(id);
		if(emailFlagsInfo.getCreatedTime()!=null) {
			JSONObject createdTime = new JSONObject();
			createdTime.put(DBConstants.ID, "SystemTime 0x0039");
			createdTime.put("value", emailFlagsInfo.getCreatedTime());
			singleValueProps.put(createdTime);
		}
		if(emailFlagsInfo.getSentTime()!=null) {
			JSONObject sentTime = new JSONObject();
			sentTime.put(DBConstants.ID, "SystemTime 0x0E06");
			sentTime.put("value", emailFlagsInfo.getSentTime());
			singleValueProps.put(sentTime);
		}
		if(emailFlagsInfo.getImportance()!=null && !emailFlagsInfo.getImportance().equalsIgnoreCase("normal")) {
			timeStamp.put("importance", emailFlagsInfo.getImportance());
		}
	
		if(emailFlagsInfo.isFlagged()) {
			JSONObject flags = new JSONObject();
			flags.put("flagStatus", "flagged");
			timeStamp.put("flag", flags);
		}
		
		if(!emailFlagsInfo.isDraft()) {
			JSONObject fromEmailAddress = new JSONObject();
			fromEmailAddress.put(DBConstants.ID, "String 0x0065");
			fromEmailAddress.put("value", emailFlagsInfo.getFrom());
			singleValueProps.put(fromEmailAddress);
			JSONObject _fromEmailAddress = new JSONObject();
			_fromEmailAddress.put(DBConstants.ID, "String 0x5D02");
			_fromEmailAddress.put("value", emailFlagsInfo.getFrom());
			singleValueProps.put(_fromEmailAddress);
			JSONObject fromEmailName = new JSONObject();
			fromEmailName.put(DBConstants.ID, "String 0x0042");
			fromEmailName.put("value",  emailFlagsInfo.getFromName()==null?emailFlagsInfo.getFrom().split("@")[0]:emailFlagsInfo.getFromName());//need to set the from name based on the fetching ones
			singleValueProps.put(fromEmailName);
			if(emailFlagsInfo.getTo()!=null && !emailFlagsInfo.getTo().isEmpty()) {
				List<String>dups = new ArrayList<>();
				JSONArray array = new JSONArray();
				for(String tos : emailFlagsInfo.getTo()) {
					if(tos==null || !(tos.trim().contains(Const.ATTHERATE)) || dups.contains(tos)) {
						continue;
					}
					tos = tos.trim();
					JSONObject emailAddress = new JSONObject();
					JSONObject _to = new JSONObject();
					_to.put("address", tos);
					dups.add(tos);
					emailAddress.put("emailAddress", _to);
					array.put(emailAddress);
				}
				timeStamp.put("toRecipients", array);
			}
			JSONArray cc = new JSONArray();
			if(emailFlagsInfo.getCc()!=null && !emailFlagsInfo.getCc().isEmpty()) {
				for(String tos : emailFlagsInfo.getCc()) {
					if(tos==null || !(tos.trim().contains(Const.ATTHERATE))) {
						continue;
					}
					tos = tos.trim();
					JSONObject emailAddress = new JSONObject();
					JSONObject _to = new JSONObject();
					_to.put("address", tos);
					emailAddress.put("emailAddress", _to);
					cc.put(emailAddress);
				}
				timeStamp.put("ccRecipients", cc);
			}
			JSONArray bcc = new JSONArray();
			if(emailFlagsInfo.getBcc()!=null && !emailFlagsInfo.getBcc().isEmpty()) {
				for(String tos : emailFlagsInfo.getBcc()) {
					if(tos==null || !(tos.trim().contains(Const.ATTHERATE))) {
						continue;
					}
					tos = tos.trim();
					JSONObject emailAddress = new JSONObject();
					JSONObject _to = new JSONObject();
					_to.put("address", tos);
					emailAddress.put("emailAddress", _to);
					bcc.put(emailAddress);
				}
				timeStamp.put("bccRecipients", bcc);
			}
			JSONArray replyTo = new JSONArray();
			if(emailFlagsInfo.getReplyTo()!=null && !emailFlagsInfo.getReplyTo().isEmpty()) {
				for(String tos : emailFlagsInfo.getReplyTo()) {
					if(tos==null || !(tos.trim().contains(Const.ATTHERATE))) {
						continue;
					}
					tos = tos.trim();
					JSONObject emailAddress = new JSONObject();
					JSONObject _to = new JSONObject();
					_to.put("address", tos);
					emailAddress.put("emailAddress", _to);
					replyTo.put(emailAddress);
				}
				timeStamp.put("replyTo", replyTo);
			}
		}
		timeStamp.put("singleValueExtendedProperties", singleValueProps);
		timeStamp.put("isRead", emailFlagsInfo.isRead());
		if(emailFlagsInfo.getMessage()!=null) {
			JSONObject body = new JSONObject();
			body = emailFlagsInfo.getMessage()!=null?body.put("contentType", "HTML"):body.put("contentType", "Text");
			body.put("content", emailFlagsInfo.getMessage());
			timeStamp.put("body", body);
		}
		return timeStamp.toString();
	}

	/**
	 * For Updating TimeStamps and From,toAddress refer this <a href="https://learn.microsoft.com/en-us/openspecs/exchange_server_protocols/ms-oxprops/f6ab1613-aefe-447d-a49c-18217230b148?redirectedfrom=MSDN">MSDN</a>
	*/
	public JSONObject createBodyForTimeStampThread(EmailFlagsInfo emailFlagsInfo) {
		JSONObject timeStamp = new JSONObject();
		JSONArray singleValueProps = new JSONArray();
		JSONObject id = new JSONObject();
		id.put(DBConstants.ID, "Integer 0x0E07");
		id.put("value", "1");
		singleValueProps.put(id);
		if(emailFlagsInfo.getTo()!=null && !emailFlagsInfo.getTo().isEmpty()) {
			List<String>dups = new ArrayList<>();
			JSONArray array = new JSONArray();
			for(String tos : emailFlagsInfo.getTo()) {
				JSONObject emailAddress = new JSONObject();
				if(dups.contains(tos)) {
					continue;
				}
				JSONObject _to = new JSONObject();
				_to.put("address", tos);
				dups.add(tos);
				emailAddress.put("emailAddress", _to);
				array.put(emailAddress);
			}
			timeStamp.put("toRecipients", array);
		}
		if(emailFlagsInfo.getCc()!=null && !emailFlagsInfo.getCc().isEmpty()) {
			List<String>dups = new ArrayList<>();
			JSONArray array = new JSONArray();
			for(String tos : emailFlagsInfo.getCc()) {
				JSONObject emailAddress = new JSONObject();
				if(dups.contains(tos)) {
					continue;
				}
				JSONObject _to = new JSONObject();
				_to.put("address", tos);
				dups.add(tos);
				emailAddress.put("emailAddress", _to);
				array.put(emailAddress);
			}
			timeStamp.put("ccRecipients", array);
		}
		if(emailFlagsInfo.getBcc()!=null && !emailFlagsInfo.getBcc().isEmpty()) {
			List<String>dups = new ArrayList<>();
			JSONArray array = new JSONArray();
			for(String tos : emailFlagsInfo.getBcc()) {
				JSONObject emailAddress = new JSONObject();
				if(dups.contains(tos)) {
					continue;
				}
				JSONObject _to = new JSONObject();
				_to.put("address", tos);
				dups.add(tos);
				emailAddress.put("emailAddress", _to);
				array.put(emailAddress);
			}
			timeStamp.put("bccRecipients", array);
		}
		timeStamp.put("singleValueExtendedProperties", singleValueProps);
		timeStamp.put("isRead", emailFlagsInfo.isRead());

		return timeStamp;
	}


	public JSONObject createBodyForMailThread(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {

		if(emailFlagsInfo==null) {
			throw new MailMigrationException("EmailFlags found null while creating body");
		}
		JSONObject message = new JSONObject();
		message.put("IsReadReceiptRequested", false);
		message.put("IsDeliveryReceiptRequested", false);
		JSONArray categories = new JSONArray();
		categories.put(emailFlagsInfo.getColor());

		if(emailFlagsInfo.getAttachments()!=null && false) {
			Optional<Map<String, String>>aLinks = Optional.ofNullable(HttpUtils.getAnchorTags(emailFlagsInfo.getMessage()));
			for(AttachmentsData attachments : emailFlagsInfo.getAttachments()) {
				if(attachments.getSize()<Const.GMAIL_ATTACHMENT_LIMIT && !attachments.isInline()) {
					continue;
				}
				attachments.setCompleted(true);
				AttachmentsData data = null;
				try {
					data = uploadFile(attachments, emailFlagsInfo);
				} catch (IOException e) {
				}
				if(data!=null && aLinks.isPresent()) {
					Map<String,String> links = aLinks.get();
					for(Map.Entry<String, String> entry : links.entrySet()) {
						if(entry.getKey().contains(attachments.getId()) || entry.getValue().equals(data.getName())) {
							emailFlagsInfo.setHtmlMessage(emailFlagsInfo.getHtmlMessage().replace(StringEscapeUtils.escapeHtml4(entry.getKey()),StringEscapeUtils.escapeHtml4(data.getOdataType())));
						}
					}
				}
			}
		}
		message.put("comment", emailFlagsInfo.getMessage());
		//message.put("isRead", emailFlagsInfo.isRead());
		return message;
	}

	public JSONArray createEmailAddresses(List<String>addresses) {
		JSONArray array = new JSONArray();
		addresses.stream().distinct().forEach(tos->{

			JSONObject emailAddress = new JSONObject();
			JSONObject _to = new JSONObject();
			_to.put("address", tos);
			emailAddress.put("emailAddress", _to);
			array.put(emailAddress);
		});
		return array;
	}

	public JSONObject bodyForFileAttachments(AttachmentsData data) {
		JSONObject attach = new JSONObject();
		if(data.getOdataType()==null) {
			data.setOdataType("#microsoft.graph.fileAttachment");
		}
		data.setCompleted(true);
		attach.put("@odata.type", data.getOdataType());
		attach.put("name",data.getName());
		attach.put("isInline",false);
		//attach.put("contentType", data.getContentType());
		attach.put("contentBytes", data.getContentBytes());
		return attach;
	}


	public AttachmentsData uploadFile(AttachmentsData data,EmailFlagsInfo emailFlagsInfo) throws IOException {
		log.info("--Entered for uploading the largeFile attachemnts=="+emailFlagsInfo.getCloudId()+"-");
		AttachmentsUploadSession uploadSession = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		File temp = null;
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();
		String sessionUrl = String .format(baseURL+CREATE_FILE_URL_ADMIN, memberId,cloud.getDriveId(),data.getName());
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decodeBase64(data.getContentBytes().getBytes()));
		try {
			temp = File.createTempFile("largeAttach"+data.getName(), ".temp");

			IOUtils.copy(byteArrayInputStream, new FileOutputStream(temp));

			JSONObject attachment = new JSONObject();
			attachment.put("@microsoft.graph.conflictBehavior", "replace");
			attachment.put("name", UriUtils.encodeFragment(data.getName(), "UTF-8"));
			attachment.put("fileSystemInfo", "{\"@odata.type\": \"microsoft.graph.fileSystemInfo\"}");
			String result = ConnectUtils.postResponse(sessionUrl, acceeToken, attachment.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isNotBlank(result)) {
				uploadSession = gson.fromJson(result, AttachmentsUploadSession.class);
			}else {
				log.info("Upload session is Null--"+emailFlagsInfo.getCloudId()+"--"+emailFlagsInfo.getFolder());
				return null;
			}
			UploadSession outlookSession = new UploadSession(data.getParentFolderId(), temp, uploadSession.getUploadUrl(), uploadSession.getNextExpectedRanges());

			while(!outlookSession.isComplete()) {
				byte[] bytesToUpload = outlookSession.getChunk();
				log.info("bytesToUpload length for attachments : "+bytesToUpload.length);
				String contentRange = String.format("bytes %d-%d/%d", outlookSession.getTotalUploaded(), outlookSession.getTotalUploaded() + bytesToUpload.length - 1, outlookSession.getFile().length());
				log.info(cloud.getId()+"--session upload length : "+String.format("bytes %d-%d/%d", outlookSession.getTotalUploaded(), outlookSession.getTotalUploaded() + bytesToUpload.length - 1, outlookSession.getFile().length())
				+" Total Uploaded : "+outlookSession.getTotalUploaded()+" session file length : "+outlookSession.getFile().length());
				if (outlookSession.getTotalUploaded() + bytesToUpload.length < outlookSession.getFile().length()) {
					result =  ConnectUtils.uploadSession(outlookSession.getUploadUrl(), bytesToUpload, contentRange, bytesToUpload.length,cloud.getId());
					if(StringUtils.isNotBlank(result)) {
						uploadSession = gson.fromJson(result, AttachmentsUploadSession.class);
						outlookSession.setRanges(uploadSession.getNextExpectedRanges());
					}else{
						log.info("==fetched the blank result we can't upload the next ranges in=="+cloud.getId()+"==="+cloud.getEmail());
						break;
					}
				}else {
					result =  ConnectUtils.uploadSession(outlookSession.getUploadUrl(), outlookSession.getChunk(), contentRange, bytesToUpload.length,cloud.getId());
					outlookSession.setComplete(true);
					FileMetadata fileMetadata = gson.fromJson(result, FileMetadata.class);
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

		} catch (FileNotFoundException e) {
			log.info(ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (IOException e) {
			log.info(ExceptionUtils.getStackTrace(e));
			throw e;
		}finally {
			if(temp!=null) {
				Files.delete(temp.toPath());
			}
		}
		return null;
	}


	public String getValidAccessToken(VendorOAuthCredential credential) {
		credential = vendorOAuthCredentialRepo.findById(credential.getId());
		if(credential.getLastRefreshed()==null || LocalDateTime.now().isAfter(credential.getLastRefreshed().plusHours(1))) {
			return refreshToken(credential.getId());
		}
		return credential.getAccessToken();
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
		VendorOAuthCredential credential = new VendorOAuthCredential();
		credential.setAccessToken(tokenResult.getAccessToken());
		credential.setRefreshToken(tokenResult.getRefreshToken());
		credential.setCloudName(CLOUD_NAME.OUTLOOK);
		credential.setLastRefreshed(LocalDateTime.now());
		credential.setExpiresAt(LocalDateTime.now().plusHours(1));
		return credential;
	}

	public JSONObject createBodyForMail(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException {

		if(emailFlagsInfo==null) {
			throw new MailMigrationException("EmailFlags found null while creating body");
		}
		JSONObject body = new JSONObject();
		JSONObject message = new JSONObject();
		if(emailFlagsInfo.getFrom()!=null) {
			JSONObject from = new JSONObject();
			JSONObject _emailAddress = new JSONObject();
			from.put("emailAddress", _emailAddress);
			_emailAddress.put("address", emailFlagsInfo.getFrom());
			message.put("from", from);
		}
		message.put("subject", emailFlagsInfo.getSubject());
		message.put("bodyPreview", emailFlagsInfo.getBodyPreview());
		if(emailFlagsInfo.getImportance()!=null && !emailFlagsInfo.getImportance().equalsIgnoreCase("normal")) {
			message.put("importance", emailFlagsInfo.getImportance());
		}
		JSONArray categories = new JSONArray();
		categories.put(emailFlagsInfo.getColor());
		if(emailFlagsInfo.isFlagged()) {
			JSONObject flags = new JSONObject();
			flags.put("flagStatus", "flagged");
			message.put("flag", flags);
		}

		//TODO : for larger files the link is appending on the text so need to replace that content inside it with the destination on as anchor tag #Done
		if(emailFlagsInfo.getAttachments()!=null && false) {
			Optional<Map<String, String>>aLinks = Optional.ofNullable(HttpUtils.getAnchorTags(emailFlagsInfo.getMessage()));
			for(AttachmentsData attachments : emailFlagsInfo.getAttachments()) {
				if(attachments.getSize()<Const.GMAIL_ATTACHMENT_LIMIT && !attachments.isInline()) {
					continue;
				}
				// need to look into this
				attachments.setCompleted(true);
				AttachmentsData data = null;
				try {
					data = uploadFile(attachments, emailFlagsInfo);
				} catch (IOException e) {
				}
				if(data!=null && aLinks.isPresent()) {
					Map<String,String> links = aLinks.get();
					for(Map.Entry<String, String> entry : links.entrySet()) {
						if(entry.getKey().contains(attachments.getId()) || entry.getValue().equals(data.getName())) {
							emailFlagsInfo.setHtmlMessage(emailFlagsInfo.getHtmlMessage().replace(StringEscapeUtils.escapeHtml4(entry.getKey()),StringEscapeUtils.escapeHtml4(data.getOdataType())));
						}
					}
				}
			}
		}

		body = emailFlagsInfo.getMessage()!=null?body.put("contentType", "HTML"):body.put("contentType", "Text");
		body.put("content", emailFlagsInfo.getMessage());
		message.put("body", body);
		if(emailFlagsInfo.getTo()!=null && !emailFlagsInfo.getTo().isEmpty()) {
			JSONArray array =createEmailAddresses(emailFlagsInfo.getTo());
			message.put("toRecipients", array);
		}
		if(emailFlagsInfo.isDraft()) {
			if(emailFlagsInfo.getReplyTo()!=null && !emailFlagsInfo.getReplyTo().isEmpty()) {
				JSONArray array =createEmailAddresses(emailFlagsInfo.getReplyTo());
				message.put("replyTo", array);
			}
			if(emailFlagsInfo.getCc()!=null && !emailFlagsInfo.getCc().isEmpty()) {
				JSONArray array =createEmailAddresses(emailFlagsInfo.getCc());
				message.put("ccRecipients", array);
			}
			if(emailFlagsInfo.getBcc()!=null && !emailFlagsInfo.getBcc().isEmpty()) {
				JSONArray array =createEmailAddresses(emailFlagsInfo.getBcc());
				message.put("bccRecipients", array);
			}
		}
		if(emailFlagsInfo.isHadAttachments()  && (emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty())) {
			try {
				JSONArray attachments = new JSONArray();
				for(AttachmentsData data : emailFlagsInfo.getAttachments()) {
					if(data.isCompleted()) {
						continue;
					}
					if(data.getSize()>Const.ATTACHMENT_LIMIT) {
						emailFlagsInfo.setLargeFile(true);
						continue;
					}
					//check for the larger files more than 3 mb to 150 mb need to do upload large session
					//Refer documentation : https://learn.microsoft.com/en-us/graph/api/attachment-createuploadsession?view=graph-rest-1.0&tabs=http
					JSONObject attach = new JSONObject();
					data.setCompleted(true);
					if(data.getOdataType()!=null && data.getOdataType().equalsIgnoreCase("#microsoft.graph.itemAttachment")) {
						//# need to fix issue for the item attachments
					}else if(data.getOdataType()==null || (data.getOdataType()!=null && data.getOdataType().equalsIgnoreCase("#microsoft.graph.fileAttachment"))){
						attach = bodyForFileAttachments(data);
					}
					attachments.put(attach);
				}
				message.put("attachments", attachments);
			} catch (Exception e) {
			}
		}
		//message.put("isRead", emailFlagsInfo.isRead());
		message.put("IsReadReceiptRequested", false);
		message.put("IsDeliveryReceiptRequested", false);
		return message;
	}
	public String createBodyForCreateGroup(String name,String description,String email) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("description", description);
		jsonObject.put("displayName", name);
		jsonObject.put("mailNickname", email.split(Const.ATTHERATE)[0]);
		jsonObject.put("mailEnabled", true);
		JSONArray type = new JSONArray();
		type.put("Unified");
		jsonObject.put("groupTypes", type);
		jsonObject.put("securityEnabled", "false");
		jsonObject.put("visibility", "Private");
		JSONArray parentArray = new JSONArray();
		parentArray.put("WelcomeEmailDisabled");
		jsonObject.put("resourceBehaviorOptions", parentArray);
		return jsonObject.toString();
	}

	/**
	 * Get Drive Details for Uploading the attachments to the OneDrive
	 * Loading only once saving in Clouds so not required for everyTime
	 */
	public String getDriveDetails(String cloudId) {
		try {
			Clouds cloud = cloudsRepoImpl.findOne(cloudId);
			ConnectUtils.checkClouds(cloud);
			String url = baseURL+String.format(USER_DRIVE_DETAIL, cloud.getMemberId());
			Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
			if(admin.getCredential()==null) {
				admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
			}
			String acceeToken = getValidAccessToken(admin.getCredential());
			String result = ConnectUtils.getResponse(url, acceeToken, null, admin.getCredential().getId(), CLOUD_NAME.GMAIL,cloud.getId());
			JsonObject drive = null;
			if(StringUtils.isNotEmpty(result)) {
				drive = JsonParser.parseString(result).getAsJsonObject();
			}
			if(drive!=null) {
				String id = drive.get("id").getAsString();
				cloud.setDriveId(id);
				cloudsRepoImpl.save(cloud);
				return id;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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

	public void validateUser(EmailFlagsInfo emailFlagsInfo,Map<String,String>mappedMembers){
		if(emailFlagsInfo.getTo()!=null && !emailFlagsInfo.getTo().isEmpty()){
			emailFlagsInfo.setTo(emailFlagsInfo.getTo().stream().filter(mappedMembers::containsKey).collect(Collectors.toList()));
		}
		if(emailFlagsInfo.getCc()!=null && !emailFlagsInfo.getCc().isEmpty()){
			emailFlagsInfo.setCc(emailFlagsInfo.getCc().stream().filter(mappedMembers::containsKey).collect(Collectors.toList()));
		}
		if(emailFlagsInfo.getBcc()!=null && !emailFlagsInfo.getBcc().isEmpty()){
			emailFlagsInfo.setBcc(emailFlagsInfo.getBcc().stream().filter(mappedMembers::containsKey).collect(Collectors.toList()));
		}
	}

	public List<EmailFlagsInfo> creatDraftBatchRequest(List<EmailFlagsInfo> requests,EmailFlagsInfo emailFlagInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		List<EmailFlagsInfo> createdInfos = new ArrayList<>();
		BatchRequests batchRequests = null;
		JSONObject request = new JSONObject();
		JSONArray batchs = new JSONArray();
		if(requests.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String,EmailFlagsInfo>infosMap = new HashMap<>();
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		for(EmailFlagsInfo emailFlagsInfo : requests) {
			try {
				String originalFrom = emailFlagsInfo.getFrom();
				String mailFolder = emailFlagsInfo.getFolder().replace(" ", "");
				String fromEmail = cloud.getEmail();
				boolean fromExists = false;
				if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(originalFrom)) {
					fromEmail = originalFrom;
					fromExists = true;
				}
				
				if(!emailFlagsInfo.isDraft() && !emailFlagsInfo.isCopy()) {
					validateUser(emailFlagsInfo, mappedEmailDetails);
					if(fromExists && (emailFlagsInfo.getTo().isEmpty())) {
						emailFlagsInfo.setTo(Arrays.asList(cloud.getAdminEmailId()));
					}
				}else {
					if(!emailFlagsInfo.isDraft()) {
						emailFlagsInfo.setCc(null);
						emailFlagInfo.setBcc(null);
					}
					if(!originalFrom.equals(cloud.getEmail())) {
						fromEmail = admin.getEmail();
						emailFlagsInfo.setFrom(fromEmail);
						if(!emailFlagsInfo.isDraft()) {
							emailFlagsInfo.setTo(Arrays.asList(cloud.getEmail()));
						}
					}else {
						fromEmail = cloud.getEmail();
						if(!emailFlagsInfo.isDraft()) {
							emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
						}
					}
				}
				
				if(mailFolder.equalsIgnoreCase("sentitems") || mailFolder.equalsIgnoreCase("drafts") || emailFlagsInfo.isDraft()) {
					if(!fromExists || emailFlagsInfo.isCopy()) {
						emailFlagsInfo.setFrom(cloud.getEmail());
						fromEmail = cloud.getEmail();
					}
					//#For Drafts we are not changing any values but for the other we are changing values as admin and those mails will be deleted by EmailPurger.class From admin 
					if(!emailFlagsInfo.isDraft() && emailFlagInfo.isCopy()) {
						emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					}
				}else {
					if(!fromExists) {
						fromEmail = admin.getEmail();
					}
					emailFlagsInfo.setFrom(fromEmail);
				}
				if(emailFlagsInfo.isDeltaThread()&& emailFlagInfo.isCopy()) {
					fromEmail = cloud.getEmail();
					emailFlagsInfo.setFrom(fromEmail);
					emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					fromExists = true;
				}
				if(!emailFlagsInfo.isCopy()) {
					validateThreadInDestination(cloud, emailFlagsInfo, originalFrom, fromExists);
				}else {
					validateThreadInDestination(cloud, emailFlagsInfo, fromEmail, fromExists);
				}
				JSONObject headers = new JSONObject();
				headers.put("Content-Type", "application/json");
				if(emailFlagsInfo.isThread() && !fromExists) {
					fromEmail = admin.getEmail();
					emailFlagsInfo.setFrom(fromEmail);
				}
				JSONObject input = emailFlagsInfo.isThread()?createBodyForMailThread(emailFlagsInfo) : createBodyForMail(emailFlagsInfo);
				String folderUrl = emailFlagsInfo.isThread() ?  String .format(USERS+"/%s/messages/%s", fromEmail,emailFlagsInfo.getThreadId())+"/createReply"
						:String.format(USERS+"/%s/messages", fromEmail);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", emailFlagsInfo.getId());
				jsonObject.put("body", input);
				jsonObject.put("headers", headers);
				jsonObject.put("method", "POST");
				jsonObject.put("url", folderUrl);
				infosMap.put(emailFlagsInfo.getId(), emailFlagsInfo);
				batchs.put(jsonObject);
			} catch (Exception e) {
				emailFlagsInfo.setConflict(true);
				emailFlagsInfo.setMessage(ExceptionUtils.getStackTrace(e));
				createdInfos.add(emailFlagsInfo);
			} 
		}
		request.put("requests", batchs);
		String result = ConnectUtils.postResponse(BATCH_OPERATION, acceeToken, request.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			batchRequests = gson.fromJson(result, BatchRequests.class);
		}	
		if(ObjectUtils.isNotEmpty(batchRequests) && !batchRequests.getResponses().isEmpty()) {
			List<EmailFlagsInfo>threadsBatch = new ArrayList<>();
			for(Response response : batchRequests.getResponses()) {
				EmailFlagsInfo info = infosMap.get(response.getId());
				if(response.getStatus()==HttpStatus.CREATED.value() && response.getValue()!=null) {
					EmailFlagsInfo _info = createFlagsInfoInBatchReq(response,emailFlagInfo.getFolder());
					_info.setLargeFile(info.isLargeFile());
					_info.setAttachments(info.getAttachments());
					if(info.isThread()) {
						_info.setTo(info.getTo());
						_info.setFolder(emailFlagInfo.getFolder());
						_info.setCc(info.getCc());
						_info.setBcc(info.getBcc());
						_info.setThread(true);
						_info.setFrom(info.getFrom());
						_info.setCopy(info.isCopy());
						threadsBatch.add(_info);
						
					}
					if(info.isThread() && info.isHadAttachments() && null!=info.getAttachments() && !info.getAttachments().isEmpty()) {
						try {
							EmailFlagsInfo attchFlags = new EmailFlagsInfo();
							attchFlags.setId(_info.getDestId());
							attchFlags.setCloudId(cloud.getId());
							attchFlags.setFrom(_info.getFrom());
							attchFlags.setAttachments(info.getAttachments());
							attchFlags.setCopy(info.isCopy());
							addAttachment(attchFlags, false);
						} catch (Exception e) {
							log.error(ExceptionUtils.getStackTrace(e));
						}
					}
					createdInfos.add(_info);
				}else {
					info.setMessage(response.getValue().getError().getCode()+":"+response.getValue().getError().getMessage());
					info.setConflict(true);
					createdInfos.add(info);
				}
			}
			if(!threadsBatch.isEmpty()) {
				creatThreadDraftBatchRequest(threadsBatch, emailFlagInfo);
			}
		}
		return createdInfos;
	}


	private void validateThreadInDestination(Clouds cloud, EmailFlagsInfo emailFlagsInfo, String originalFrom,
			boolean fromExists) throws Exception {
		if(emailFlagsInfo.isThread()) {
			EmailFlagsInfo info = new EmailFlagsInfo();
			info.setConvIndex(emailFlagsInfo.getConvIndex());
			info.setThread(emailFlagsInfo.isThread());
			info.setSubject(emailFlagsInfo.getSubject());
			if(fromExists) {
				info.setCloudId(cloud.getId());
				if(!originalFrom.equals(cloud.getEmail())) {
					Clouds fromCloud = cloudsRepoImpl.findCloudsByEmailId(cloud.getUserId(), originalFrom);
					if(null!=fromCloud) {
						info.setCloudId(fromCloud.getId());
					}
				}
			}else {
				info.setCloudId(cloud.getAdminCloudId());
			}
			Value value = getSingleMailByConversationId(info);
			if(value!=null) {
				emailFlagsInfo.setThreadId(value.getId());
			}
		}
	}
	
	
	public List<EmailFlagsInfo> creatThreadDraftBatchRequest(List<EmailFlagsInfo> requests,EmailFlagsInfo emailFlagInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		List<EmailFlagsInfo> createdInfos = new ArrayList<>();
		BatchRequests batchRequests = null;
		JSONObject request = new JSONObject();
		JSONArray batchs = new JSONArray();
		if(requests.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String,EmailFlagsInfo>infosMap = new HashMap<>();
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		for(EmailFlagsInfo emailFlagsInfo : requests) {
			try {
				String originalFrom = emailFlagsInfo.getFrom();
				String mailFolder = emailFlagsInfo.getFolder()==null?DEFAULT_MAILBOX:emailFlagsInfo.getFolder().replace(" ", "");
				String fromEmail = cloud.getEmail();
				boolean fromExists = false;
				if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(originalFrom)) {
					fromEmail = originalFrom;
					fromExists = true;
				}
				if(!emailFlagsInfo.isDraft() && !emailFlagsInfo.isCopy()) {
					validateUser(emailFlagsInfo, mappedEmailDetails);
				}else {
					if(!emailFlagsInfo.isDraft()) {
						emailFlagsInfo.setCc(null);
						emailFlagInfo.setBcc(null);
					}
					if(!originalFrom.equals(cloud.getEmail())) {
						fromEmail = admin.getEmail();
						emailFlagsInfo.setTo(Arrays.asList(cloud.getEmail()));
					}else {
						fromEmail = cloud.getEmail();
						emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					}
				}
				if(mailFolder.equalsIgnoreCase("sentitems") || mailFolder.equalsIgnoreCase("drafts")) {
					emailFlagsInfo.setFrom(cloud.getEmail());
					if(!mailFolder.equalsIgnoreCase("drafts")) {
						emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					}
					//#For Drafts we are not changing any values but for the other we are changing values as admin and those mails will be deleted by EmailPurger.class From admin 
					if(emailFlagsInfo.getTo().isEmpty()) {
						emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					}
				}else {
					if(!fromExists) {
						fromEmail = admin.getEmail();
					}
					emailFlagsInfo.setFrom(fromEmail);
					if(emailFlagsInfo.getTo().isEmpty()) {
						emailFlagsInfo.setTo(Arrays.asList(cloud.getEmail()));
					}
				}
				if(emailFlagsInfo.isDeltaThread() && emailFlagInfo.isCopy()) {
					fromEmail = cloud.getEmail();
					emailFlagsInfo.setFrom(fromEmail);
					emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					fromExists = true;
				}
				JSONObject headers = new JSONObject();
				headers.put("Content-Type", "application/json");
				if(emailFlagsInfo.isThread() && !fromExists) {
					fromEmail = cloud.getEmail();
					emailFlagsInfo.setFrom(fromEmail);
				}
				JSONObject input = createBodyForTimeStampThread(emailFlagsInfo);
				String folderUrl = String .format(USERS+"/%s/messages/%s", fromEmail,emailFlagsInfo.getDestId());
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", emailFlagsInfo.getId());
				jsonObject.put("body", input);
				jsonObject.put("headers", headers);
				jsonObject.put("method", "PATCH");
				jsonObject.put("url", folderUrl);
				infosMap.put(emailFlagsInfo.getId(), emailFlagsInfo);
				batchs.put(jsonObject);
			} catch (Exception e) {
				emailFlagsInfo.setConflict(true);
				emailFlagsInfo.setMessage(ExceptionUtils.getStackTrace(e));
				createdInfos.add(emailFlagsInfo);
			} 
		}
		request.put("requests", batchs);
		String result = ConnectUtils.postResponse(BATCH_OPERATION, acceeToken, request.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			batchRequests = gson.fromJson(result, BatchRequests.class);
		}	
		if(ObjectUtils.isNotEmpty(batchRequests) && !batchRequests.getResponses().isEmpty()) {
			for(Response response : batchRequests.getResponses()) {
				if(response.getStatus()==HttpStatus.OK.value() && response.getValue()!=null) {
					createdInfos.add(createFlagsInfoInBatchReq(response,emailFlagInfo.getFolder()));
				}else {
					EmailFlagsInfo info = infosMap.get(response.getId());
					info.setMessage(response.getValue().getError().getCode()+":"+response.getValue().getError().getMessage());
					info.setConflict(true);
					createdInfos.add(info);
				}
			}
		}
		return createdInfos;
	}



	public List<EmailFlagsInfo> creatSendBatchRequest(List<EmailFlagsInfo> requests,EmailFlagsInfo emailFlagInfo) {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		}
		List<EmailFlagsInfo> createdInfos = new ArrayList<>();
		BatchRequests batchRequests = null;
		JSONObject request = new JSONObject();
		JSONArray batchs = new JSONArray();
		if(requests.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String,EmailFlagsInfo>infosMap = new HashMap<>();
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		for(EmailFlagsInfo emailFlagsInfo : requests) {
			try {
				String originalFrom = emailFlagsInfo.getFrom();
				String mailFolder = emailFlagsInfo.getFolder().replace(" ", "");
				String fromEmail = cloud.getEmail();
				boolean fromExists = false;
				if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(originalFrom)) {
					fromEmail = originalFrom;
					fromExists = true;
				}
				if(mailFolder.equalsIgnoreCase("sentitems") || originalFrom.equalsIgnoreCase(cloud.getEmail())) {
					emailFlagsInfo.setFrom(cloud.getEmail());
					fromEmail = cloud.getEmail();
				}else {
					if(!fromExists || emailFlagsInfo.isCopy()) {
						fromEmail = admin.getEmail();
						mailFolder = DEFAULT_MAILBOX;
					}
					emailFlagsInfo.setFrom(fromEmail);
				}
				if(emailFlagsInfo.isDeltaThread() && emailFlagInfo.isCopy()) {
					fromEmail = cloud.getEmail();
					emailFlagsInfo.setFrom(fromEmail);
					emailFlagsInfo.setTo(Arrays.asList(admin.getEmail()));
					fromExists = true;
				}
				if(emailFlagsInfo.isThread() && !fromExists) {
					fromEmail = admin.getEmail();
					emailFlagsInfo.setFrom(fromEmail);
				}
				JSONObject headers = new JSONObject();
				headers.put("Content-Type", "application/json");
				JSONObject input = new JSONObject();
				String folderUrl = String.format(USERS+"/%s/messages/%s/send", fromEmail,emailFlagsInfo.getDestId());
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", emailFlagsInfo.getId());
				jsonObject.put("body", input);
				jsonObject.put("headers", headers);
				jsonObject.put("method", "POST");
				jsonObject.put("url", folderUrl);
				batchs.put(jsonObject);
				infosMap.put(String.valueOf(emailFlagsInfo.getId()), emailFlagsInfo);
			} catch (Exception e) {
				emailFlagsInfo.setConflict(true);
				emailFlagsInfo.setMessage(ExceptionUtils.getStackTrace(e));
				createdInfos.add(emailFlagsInfo);
			} 
		}
		request.put("requests", batchs);
		String result = ConnectUtils.postResponse(BATCH_OPERATION, acceeToken, request.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
		if(StringUtils.isNotEmpty(result)) {
			batchRequests = gson.fromJson(result, BatchRequests.class);
		}	
		if(ObjectUtils.isNotEmpty(batchRequests) && !batchRequests.getResponses().isEmpty()) {
			for(Response response : batchRequests.getResponses()) {
				if(response.getStatus()==HttpStatus.ACCEPTED.value()) {
					createdInfos.add(infosMap.get(response.getId()));
				}else {
					EmailFlagsInfo info = infosMap.get(response.getId());
					if(null!=info) {
						info.setMessage(response.getValue().getError().getCode()+":"+response.getValue().getError().getMessage());
						info.setConflict(true);
					}
					createdInfos.add(info);
				}
			}
		}
		return createdInfos;
	}
	
	private EmailFlagsInfo createFlagsInfoInBatchReq(Response response,String mailFolder) {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setThreadId(response.getValue().getConversationId());
		emailFlagsInfo.setDestParent(response.getValue().getParentFolderId());
		emailFlagsInfo.setConvIndex(response.getValue().getInternetMessageId());
		emailFlagsInfo.setId(response.getId());
		emailFlagsInfo.setDestId(response.getValue().getId());
		emailFlagsInfo.setFrom(response.getValue().getFrom().getEmailAddress().getAddress());
		emailFlagsInfo.setFolder(mailFolder);
		return emailFlagsInfo;
	}
	
	
	
	private String createDisplayName(EMailRules eMailRules) {
		if(eMailRules.getDisplayName()!=null) {
			return eMailRules.getDisplayName();
		}
		try {
			String name = " ";
			if(eMailRules.getFromAddresses() != null && !eMailRules.getFromAddresses().isEmpty()) {
				name =name+" From :"+eMailRules.getFromAddresses().get(0);
			}
			if(eMailRules.getSentToAddresses()!=null && !eMailRules.getSentToAddresses().isEmpty()) {
				name = name+" To :"+eMailRules.getSentToAddresses().get(0);
			}
			if(eMailRules.getSubjectContains()!=null) {
				name = name+" Subject contains :"+eMailRules.getSubjectContains();
			}
			if(eMailRules.isNegotiation()) {
				name = name+" Doesn't contains :"+eMailRules.getNegatedQuery();
			}
			if(null!=eMailRules.getQuery()) {
				name = name+" Body Contains :"+eMailRules.getQuery();
			}
			if(eMailRules.isAttachments()) {
				name = name+" hasAttachments :"+(eMailRules.getMinimumSize()>0?("smaller :"+eMailRules.getMinimumSize()):""+(eMailRules.getMaximumSize()>0? ("larger :"+eMailRules.getMaximumSize()):""));
			}
			if(eMailRules.getMailFolderName()!=null) {
				name = name+" MoveTo Folder :"+eMailRules.getMailFolderName();
			}
			return name;
		} catch (Exception e) {
		}
		return null;
	}
	
	
	public Value getSingleMailByConversationId(EmailFlagsInfo emailFlagsInfo) throws Exception {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		EmailList emailList = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken) || null==emailFlagsInfo.getConvIndex()) { 
			return null; 
		}

		int count =0;
		try {
			String convId = emailFlagsInfo.getConvIndex();
			boolean tryFolders = true;
			String queryId = "internetMessageId";
			while(tryFolders) {
				String url1 = baseURL+"users/"+memberId+"/messages";
				url1 = url1+"?$filter=" +queryId+" eq '"+convId+"'";
				count = count+1;
				String result = ConnectUtils.getResponse(url1, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				emailList = gson.fromJson(result, EmailList.class);
				if(!ObjectUtils.isEmpty(emailList) &&(emailList!=null && !emailList.getValue().isEmpty())) {
					return getUnModifiedValue(emailList.getValue(), emailFlagsInfo.getConvIndex());
				}else if(emailFlagsInfo.getSubject()!=null){
					Value val = getSingleMailBySubject(emailFlagsInfo);
					if(val!=null) {
						return val;
					}
					if(count >2)
						throw new Exception("mail not found in the Messages :");
				}else {
					if(count >2)
						throw new Exception("mail not found in the Messages :");
				}
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}
		return null;
	}
	
	public CalenderViewValue getEventByConversationId(CalenderFlags emailFlagsInfo) throws Exception {
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		CalenderViews emailList = null;
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken) || null==emailFlagsInfo.getICalUId()) { 
			return null; 
		}

		int count =0;
		try {
			String convId = emailFlagsInfo.getICalUId();
			boolean tryFolders = true;
			String queryId = "iCalUId";
			while(tryFolders) {
				if(count>1) {
					break;
				}
				String url1 = baseURL+"users/"+memberId+"/events?";
				url1 = url1+"$filter=+"+queryId+" eq '"+convId+"'";
				count = count+1;
				String result = ConnectUtils.getResponse(url1, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				emailList = gson.fromJson(result, CalenderViews.class);
				if(!ObjectUtils.isEmpty(emailList) &&(emailList!=null && !emailList.getValue().isEmpty())) {
					return getUnModifiedEvent(emailList.getValue(), emailFlagsInfo.getICalUId());
				}
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}
		return null;
	}

	public Value getSingleMailBySubject(EmailFlagsInfo emailFlagsInfo) throws Exception {
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
		String mailFolder = emailFlagsInfo.getFolder();
		if(StringUtils.isNotBlank(mailFolder) && (mailFolder.equalsIgnoreCase("sentitems") || mailFolder.equalsIgnoreCase("drafts"))) {
			mailFolder = mailFolder.replace(" ", "");
		}else {
			mailFolder = DEFAULT_MAILBOX;
		}
		if(emailFlagsInfo.isDeleted() &&  emailFlagsInfo.getFolder().equalsIgnoreCase("deleteditems")) {
			mailFolder = emailFlagsInfo.getFolder();
		}

		int count =0;
		try {
			String convId = URLEncoder.encode(emailFlagsInfo.getSubject(), StandardCharsets.UTF_8.toString());
			convId  = URI.create(convId).toASCIIString();
			boolean tryFolders = true;
			String queryId = "subject";
			while(tryFolders) {
				String url1 = baseURL+"users/"+memberId+"/messages";
				url1 = url1+"?$filter=" +queryId+" eq '"+convId+"'";
				log.info("==url for finding the sent email==="+url1);
				count = count+1;
				String result = ConnectUtils.getResponse(url1, acceeToken, null,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
				emailList = gson.fromJson(result, EmailList.class);
				if(!ObjectUtils.isEmpty(emailList) &&(emailList!=null && !emailList.getValue().isEmpty())) {
					return emailList.getValue().get(emailList.getValue().size()-1);
				}else {
					return null;
				}
			}
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}
		return null;
	}


	private Value getUnModifiedValue(List<Value> values,String fromEmail) {
		if(!values.isEmpty()) {
			for(Value value : values) {
				if(fromEmail.equals(value.getInternetMessageId())) {
					return value;
				}
			}
		}
		return null;
	}
	
	
	private CalenderViewValue getUnModifiedEvent(List<CalenderViewValue> values,String fromEmail) {
		if(!values.isEmpty()) {
			for(CalenderViewValue value : values) {
				if(fromEmail.equals(value.getICalUId())) {
					return value;
				}
			}
		}
		return null;
	}
	
	
	private Value getUnModifiedValueEmail(List<Value> values,String fromEmail) {
		if(!values.isEmpty()) {
			for(Value value : values) {
				if(fromEmail.equals(value.getSender().getEmailAddress().getAddress())) {
					return value;
				}
			}
		}
		return null;
	}
	
	public ContactInfo createContact(List<ContactsFlagInfo> contactsFlagsInfo,ContactsFlagInfo contactsFlagInfo ) {
		Clouds cloud = cloudsRepoImpl.findOne(contactsFlagInfo.getCloudId());
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
		BatchRequests batchRequests = null;
		JSONObject request = new JSONObject();
		JSONArray batchs = new JSONArray();
		for(ContactsFlagInfo info : contactsFlagsInfo) {
			String folderUrl =String .format(USERS+"/%s/contacts", memberId);
			//String input = contactsHelper.createBodyForContact(contacts)
		}
		return null;
		
	}
	
	
	public List<String> addAttachment(EmailFlagsInfo emailFlagsInfo,boolean event) throws IOException {

		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		EmailAttachMentValue emailAttachMentValue = null;
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		String  acceeToken= getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();

		if(StringUtils.isBlank(acceeToken)) { 
			return Collections.emptyList(); 
		} 
		String mailFolder = emailFlagsInfo.getFolder();
		if(StringUtils.isNotBlank(mailFolder)) {
			mailFolder = mailFolder.replace(" ", "");
		}else {
			mailFolder = DEFAULT_MAILBOX;
		}

		boolean fromExists = false;
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(emailFlagsInfo.getFrom())) {
			fromExists = true;
		}
		if(fromExists) {
			memberId = mappedEmailDetails.get(emailFlagsInfo.getFrom());
			if(emailFlagsInfo.isCopy() && !event) {
				if(emailFlagsInfo.getFrom().equals(cloud.getEmail())) {
					memberId = cloud.getMemberId();
				}else {
					memberId = admin.getMemberId();
				}
			}else {
				memberId = mappedEmailDetails.get(emailFlagsInfo.getFrom());
			}
		}else {
			memberId = admin.getMemberId();
		}
		
		String url = String .format(ADD_ATTACHMENT, memberId);
		if(event) {
			url = String.format(baseURL+GET_CALENDER_EVENTS, memberId,emailFlagsInfo.getFolder());
		}
		url= url+"/"+emailFlagsInfo.getId()+"/attachments";
		List<String> aIds = new ArrayList<>();
		log.info("URL for appending attachments :"+url);
		if(emailFlagsInfo.getAttachments()!=null && !emailFlagsInfo.getAttachments().isEmpty()) {
			try {
				for(AttachmentsData attachmentsData : emailFlagsInfo.getAttachments()) {
					if(attachmentsData.isLargeFile()) {
						//Here for uploading largeFile attachments more than 3MB from the graph docs
						log.info("==For uploading large Attachemnt ==="+attachmentsData.getName()+"=="+cloud.getEmail()+"=="+cloud.getUserId());
						String aId = uploadLargeFile(attachmentsData, emailFlagsInfo,event);
						log.info("==LargeFileUploaded==="+aId+"===cloudId=="+cloud.getId());
						aIds.add(aId);
					} else if(!attachmentsData.isCompleted()){ 
						String input = bodyForFileAttachments(attachmentsData).toString(); 
						String result = ConnectUtils.postResponse(url, acceeToken, input,admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
						if(StringUtils.isNotBlank(result)) { 
							emailAttachMentValue = gson.fromJson(result, EmailAttachMentValue.class);
							if(emailAttachMentValue!=null) { 
								String aId = emailAttachMentValue.getId();
								aIds.add(aId);
							}
						}
					}
				}
			} catch (Exception e) {
			} 
		}
		return aIds;
	}
	
	
	public String uploadLargeFile(AttachmentsData data,EmailFlagsInfo emailFlagsInfo,boolean event) throws IOException {
		log.info("--Entered for uploading the largeFile attachemnts=="+emailFlagsInfo.getCloudId()+"-");
		AttachmentsUploadSession uploadSession = null;
		Clouds cloud = cloudsRepoImpl.findOne(emailFlagsInfo.getCloudId());
		ConnectUtils.checkClouds(cloud);
		Clouds admin = cloudsRepoImpl.findAdmin(cloud.getAdminMemberId(), cloud.getUserId());
		if(admin.getCredential()==null) {
			admin.setCredential(vendorOAuthCredentialRepo.findById(admin.getEmail()+":"+admin.getCloudName()));
		}
		File temp = null;
		String acceeToken = getValidAccessToken(admin.getCredential());
		String memberId=cloud.getMemberId();
		boolean fromExists = false;
		Map<String,String>mappedEmailDetails = getMemberDetails(cloud.getUserId(), admin.getAdminCloudId());
		if(!mappedEmailDetails.isEmpty() && mappedEmailDetails.containsKey(emailFlagsInfo.getFrom())) {
			fromExists = true;
		}
		if(fromExists) {
			if(emailFlagsInfo.isCopy()) {
				if(emailFlagsInfo.getFrom().equals(cloud.getEmail())) {
					memberId = cloud.getMemberId();
				}else {
					memberId = admin.getMemberId();
				}
			}else {
				memberId = mappedEmailDetails.get(emailFlagsInfo.getFrom());
			}
		}else {
			memberId = admin.getMemberId();
		}
		String sessionUrl = String .format(ADD_ATTACHMENT, memberId);
		if(event) {
			sessionUrl = String.format(baseURL+GET_CALENDER_EVENTS, memberId,emailFlagsInfo.getFolder());
		}
		sessionUrl = sessionUrl+"/"+emailFlagsInfo.getId()+"/attachments/createUploadSession";
		JSONObject session = new JSONObject();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decodeBase64(data.getContentBytes().getBytes()));
		try {
			temp = File.createTempFile("largeAttach"+data.getName(), ".temp");

			IOUtils.copy(byteArrayInputStream, new FileOutputStream(temp));
			String attachmentId = null;

			JSONObject attachment = new JSONObject();
			attachment.put("attachmentType", "file");
			attachment.put("name", data.getName());
			attachment.put("size",temp.length());
			session.put("AttachmentItem", attachment);
			String result = ConnectUtils.postResponse(sessionUrl, acceeToken, session.toString(),admin.getCredential().getId(), CLOUD_NAME.OUTLOOK,cloud.getId());
			if(StringUtils.isNotBlank(result)) {
				uploadSession = gson.fromJson(result, AttachmentsUploadSession.class);
			}else {
				log.info("Upload session is Null--"+emailFlagsInfo.getCloudId()+"--"+emailFlagsInfo.getFolder());
				return null;
			}
			UploadSession outlookSession = new UploadSession(data.getParentFolderId(), temp, uploadSession.getUploadUrl(), uploadSession.getNextExpectedRanges());

			while(!outlookSession.isComplete()) {
				byte[] bytesToUpload = outlookSession.getChunk();
				log.info("bytesToUpload length for attachments : "+bytesToUpload.length);
				String contentRange = String.format("bytes %d-%d/%d", outlookSession.getTotalUploaded(), outlookSession.getTotalUploaded() + bytesToUpload.length - 1, outlookSession.getFile().length());
				log.info(cloud.getId()+"--session upload length : "+String.format("bytes %d-%d/%d", outlookSession.getTotalUploaded(), outlookSession.getTotalUploaded() + bytesToUpload.length - 1, outlookSession.getFile().length())
				+" Total Uploaded : "+outlookSession.getTotalUploaded()+" session file length : "+outlookSession.getFile().length());
				if (outlookSession.getTotalUploaded() + bytesToUpload.length < outlookSession.getFile().length()) {
					result =  ConnectUtils.uploadSession(outlookSession.getUploadUrl(), bytesToUpload, contentRange, bytesToUpload.length,cloud.getId());
					if(StringUtils.isNotBlank(result)) {
						uploadSession = gson.fromJson(result, AttachmentsUploadSession.class);
						outlookSession.setRanges(uploadSession.getNextExpectedRanges());
					}else{
						log.info("==fetched the blank result we can't upload the next ranges in=="+cloud.getId()+"==="+cloud.getEmail());
						break;
					}
				}else {
					result =  ConnectUtils.uploadSession(outlookSession.getUploadUrl(), outlookSession.getChunk(), contentRange, bytesToUpload.length,cloud.getId());
					outlookSession.setComplete(true);
					attachmentId = result;
					log.info(cloud.getId()+"===After uploading large file===attahcment is=="+attachmentId);
					return attachmentId;
				}
			}

		} catch (FileNotFoundException e) {
			log.info(ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (IOException e) {
			log.info(ExceptionUtils.getStackTrace(e));
			throw e;
		}finally {
			if(temp!=null) {
				try {
					Files.delete(temp.toPath());
				} catch (Exception e) {
				}
			}
		}
		return null;
	}
	
	public Contacts convertContacts(ContactValue contactValue) {
		Contacts contacts = new Contacts();
		contacts.setFirstName(contactValue.getGivenName());
		contacts.setLastName(contactValue.getSurname());
		contacts.setCompanyName(contactValue.getCompanyName());
		contacts.setNotes(contactValue.getPersonalNotes());
		PhoneNumbers phoneNumbers = new PhoneNumbers();
		phoneNumbers.setPhoneNo(contactValue.getMobilePhone());
		contacts.setPhoneNumbers(Arrays.asList(phoneNumbers));
		List<Emails> mailIds = new ArrayList<>();
		contactValue.getEmailAddresses().forEach(address->{
			Emails emailAddresses = new Emails();
			emailAddresses.setEmailAddress(address.getAddress());
			emailAddresses.setName(address.getName());
			mailIds.add(emailAddresses);
		});
		contacts.setEmailAddresses(mailIds);
		contacts.setId(contactValue.getId());
		return contacts;
	}
	private String removeTeamsMeetingLink(Document doc) { 
		Element blockquoteElement = doc.select("blockquote").first();
		if (blockquoteElement != null) {
			return blockquoteElement.html();  // Get the text content between the opening and closing tags
		}
		return doc.html(); 
	}
	
}
