package com.testing.mail;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.GMailConnector;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.impl.helper.GmailHelper;
import com.testing.mail.connectors.management.CloudSaveTask;
import com.testing.mail.connectors.management.EmailPurger;
import com.testing.mail.connectors.management.GroupsSaveTask;
import com.testing.mail.contacts.dao.ContactsFlagInfo;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.RateLimitConfigurer;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailJobDetails;
import com.testing.mail.repo.entities.EmailJobDetails.REPORT_STATUS;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.rest.CustomScriptMgmtService;
import com.testing.mail.rest.MailConnectorService;
import com.testing.mail.rest.MailMappingConnector;
import com.testing.mail.rest.MailMigrationMgmtService;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.EventRangeUtils;
import com.testing.mail.utils.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

@SpringBootTest
public class ClouFuzeEmailMigrationTest {

	@Autowired
	OutLookMailConnector lookMailConnector;

	@Autowired
	GMailConnector gMailConnector;
	
	@Autowired
	GmailHelper gmailHelper; 
	
	@Autowired
	DBConnectorService connectorService;
	@Autowired
	MailMigrationMgmtService mailMigrationMgmtService;
	@Autowired
	MailServiceFactory mailServiceFactory;

	@Autowired
	MailConnectorService mailConnectorService;
	@Autowired
	CustomScriptMgmtService customScript;

	@Autowired
	MailMappingConnector mailMappingConnector;
	@Autowired
	MongoTemplate mongoTemplate;


	@Test
	void testFetchMailFolder() {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setCloudId("657acc1dacbcfc3426f8904b");
		emailFlagsInfo.setFolder("inbox");
		List<EmailFlagsInfo> infos = lookMailConnector.getListOfMailFolders(emailFlagsInfo);
		long count = infos.stream()
				.mapToLong(EmailFlagsInfo::getSizeInBytes)
				.sum();
		System.out.println(emailFlagsInfo.getCloudId()+"--"+count);
		Assertions.assertNotNull(infos);
	}
	@Test
	void testFetchContacts() {
		ContactsFlagInfo contactsFlagInfo = new ContactsFlagInfo();
		contactsFlagInfo.setCloudId("657c2caba532181c873af3a1");
		lookMailConnector.listContacts(contactsFlagInfo);
	}
	@Test
	void testSaveWorkSpace() {
		EmailWorkSpace emailWorkSpace = new EmailWorkSpace();
		connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
	}
	@Test
	void testCloudAdding() {
		Clouds clouds = new Clouds();
		clouds.setEmail("granger@gajha.com");
		clouds.setMemberId("cb41c31d-c08a-4fe0-91ab-283470949755");
		String accessToken = "eyJ0eXAiOiJKV1QiLCJub25jZSI6IjNyZ29CUndhYnNpMVRTREV4aWRWVmN4dWhLQUtuSlVwTUpkNWRhejZYNkkiLCJhbGciOiJSUzI1NiIsIng1dCI6IlQxU3QtZExUdnlXUmd4Ql82NzZ1OGtyWFMtSSIsImtpZCI6IlQxU3QtZExUdnlXUmd4Ql82NzZ1OGtyWFMtSSJ9.eyJhdWQiOiJodHRwczovL2dyYXBoLm1pY3Jvc29mdC5jb20iLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8wZGU2ZDIxMC1hYzk0LTQ2MWQtYTkzNS00ZjZjMTA1MjM5YTQvIiwiaWF0IjoxNzAyMzEyOTE5LCJuYmYiOjE3MDIzMTI5MTksImV4cCI6MTcwMjMxNjgxOSwiYWlvIjoiRTJWZ1lFZzhPamsvOWVQRW16TmI1OTJmZGVSWUFBQT0iLCJhcHBfZGlzcGxheW5hbWUiOiJDb25uZWN0X2FkZG9uIiwiYXBwaWQiOiIwNjhiYjY5Zi01MmZhLTQxMDQtYmM5OC1kYTQxMDUzYzA0MGYiLCJhcHBpZGFjciI6IjEiLCJpZHAiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8wZGU2ZDIxMC1hYzk0LTQ2MWQtYTkzNS00ZjZjMTA1MjM5YTQvIiwiaWR0eXAiOiJhcHAiLCJvaWQiOiIwNGE0YjFlNC0zYzZhLTQ3ZTctYjFkOC1jODUzMWQwN2Y4ZDgiLCJyaCI6IjAuQVZBQUVOTG1EWlNzSFVhcE5VOXNFRkk1cEFNQUFBQUFBQUFBd0FBQUFBQUFBQUJRQUFBLiIsInJvbGVzIjpbIk1haWwuUmVhZFdyaXRlIiwiVXNlci5SZWFkV3JpdGUuQWxsIiwiRG9tYWluLlJlYWRXcml0ZS5BbGwiLCJEZWxlZ2F0ZWRQZXJtaXNzaW9uR3JhbnQuUmVhZFdyaXRlLkFsbCIsIk1haWwuUmVhZEJhc2ljLkFsbCIsIkFkbWluaXN0cmF0aXZlVW5pdC5SZWFkLkFsbCIsIkRpcmVjdG9yeS5SZWFkV3JpdGUuQWxsIiwiU2l0ZXMuUmVhZC5BbGwiLCJTaXRlcy5SZWFkV3JpdGUuQWxsIiwiR3JvdXAuUmVhZFdyaXRlLkFsbCIsIkZpbGVzLlJlYWRXcml0ZS5BbGwiLCJUZWFtTWVtYmVyLlJlYWRXcml0ZS5BbGwiLCJPcmdhbml6YXRpb24uUmVhZFdyaXRlLkFsbCIsIkNhbGVuZGFycy5SZWFkV3JpdGUiLCJBY2Nlc3NSZXZpZXcuUmVhZC5BbGwiLCJDcm9zc1RlbmFudFVzZXJQcm9maWxlU2hhcmluZy5SZWFkV3JpdGUuQWxsIiwiTWFpbC5TZW5kIiwiVXNlci5NYW5hZ2VJZGVudGl0aWVzLkFsbCIsIk1haWxib3hTZXR0aW5ncy5SZWFkV3JpdGUiLCJBdWRpdExvZy5SZWFkLkFsbCIsIlNpdGVzLkZ1bGxDb250cm9sLkFsbCIsIlJlcG9ydHMuUmVhZC5BbGwiXSwic3ViIjoiMDRhNGIxZTQtM2M2YS00N2U3LWIxZDgtYzg1MzFkMDdmOGQ4IiwidGVuYW50X3JlZ2lvbl9zY29wZSI6Ik5BIiwidGlkIjoiMGRlNmQyMTAtYWM5NC00NjFkLWE5MzUtNGY2YzEwNTIzOWE0IiwidXRpIjoiNDhsek43eGZXRWFMVnhZYVMxT0ZBQSIsInZlciI6IjEuMCIsIndpZHMiOlsiMDk5N2ExZDAtMGQxZC00YWNiLWI0MDgtZDVjYTczMTIxZTkwIl0sInhtc190Y2R0IjoxNjMxNTI1NjQ5fQ.nAXXfR-cM4UIig5NF7jyMocDHmOhPxLfLqpukcabXxCbu8lUWe-Twu3gLySnLNsgHXn49EJocgwueM4B0iMFZZ1JLpXxzCeVpUz4TuplYET1Y77d649rCsPash_5CjfeZhJwyKDaJLPtTwk5q8WgTUpBSWyKsk4BEJnsqTmCHsLGJW-NGeX-B1rkFnPmaIFllZa5aLjmPel_Nccscl80qI6u7chkwV8p5a8X9HV1fyRtbAgVoADRJxdJ6wLNZE7bw8MTNNoLMsqnragvegOOkL14o7zDyvGy8Pb8JOM-tUSN0B2WnV4j5cZKLvxRf-aHEFaC-t0ivQLG1z96qEugGQ";
		new CloudSaveTask(clouds, "64ae920cdd9631217b1a9b95", accessToken, CLOUD_NAME.OUTLOOK, mailServiceFactory, connectorService.getCloudsRepoImpl(), connectorService.getAppMongoOpsManager(), connectorService.getCredentialsRepo(), gmailHelper).call();
	}

	@Test
	public void testLoop() {
		EmailWorkSpace emailWorkSpace = connectorService.getEmailInfoRepoImpl().getAggregartedResult("63dd16ede55bbd790c59a96d");
		System.out.println(emailWorkSpace.toString());
	}

	@Test
	public void testCalendarMigration() {
		EmailWorkSpace emailWorkSpaces = new EmailWorkSpace();
		emailWorkSpaces.setFromCloud(CLOUD_NAME.GMAIL);
		emailWorkSpaces.setToCloud(CLOUD_NAME.OUTLOOK);
		emailWorkSpaces.setFromMailId("dan@testing.us");
		emailWorkSpaces.setFromFolderId("/");
		emailWorkSpaces.setToMailId("dan@gajha.com");
		emailWorkSpaces.setOwnerEmailId("nagesh");
		emailWorkSpaces.setToFolderId("/");
		emailWorkSpaces.setDeltaMigration(true);
		emailWorkSpaces.setCalendar(true);

		mailMigrationMgmtService.initiateMigration(Arrays.asList(emailWorkSpaces), "64884a43a85c59573c2300dc", false);
	}


	@Test
	void testDeleting() {
		EmailWorkSpace emailWorkSpace = connectorService.getWorkSpaceRepoImpl().findOne("640ade87ab788d0c92553e71");
		EmailInfo  emailInfo = connectorService.getEmailInfoRepoImpl().findOne("640ade99ab788d0c92553ebf");
		new EmailPurger(mailServiceFactory, connectorService, emailWorkSpace, emailInfo).run();;
	}


	@Test
	void testRateLimitConfig() {
		RateLimitConfigurer config = new RateLimitConfigurer();
		config.setCloudName(CLOUD_NAME.GMAIL);
		Map<String,Integer> rateLimits = new HashMap<>();
		rateLimits.put("labels_list", 1);
		rateLimits.put("labels_get", 1);
		rateLimits.put("messages_attachments_get", 5);
		rateLimits.put("messages_batchDelete", 50);
		rateLimits.put("messages_get", 5);
		rateLimits.put("messages_insert", 25);
		rateLimits.put("messages_modify", 5);
		rateLimits.put("settings_delegates_create", 100);
		rateLimits.put("settings_delegates_delete", 5);
		rateLimits.put("settings_delegates_list", 1);
		rateLimits.put("settings_filters_create", 5);
		rateLimits.put("settings_filters_list", 1);
		rateLimits.put("settings_forwardingAddresses_list", 1);
		rateLimits.put("settings_getAutoForwarding", 1);
		rateLimits.put("settings_getImap", 1);
		rateLimits.put("settings_getPop", 1);
		rateLimits.put("settings_sendAs_list", 1);
		rateLimits.put("settings_sendAs_get", 1);
		rateLimits.put("settings_sendAs_create", 100);
		rateLimits.put("settings_sendAs_update", 100);
		rateLimits.put("threads_list", 10);
		rateLimits.put("settings_updateVacation", 5);
		rateLimits.put("labels_create", 5);
		config.setRateLimits(rateLimits);
		connectorService.getAppMongoOpsManager().saveRateLimiter(config);
	}
	@Test
	void testMetadata() throws Exception {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId("64d352bde5b5e80198fecc67");
		emailFlagsInfo.setOrganizer("alex@gajha.com");
		emailFlagsInfo.setFromName("Dan d");
		emailFlagsInfo.setId("AAMkAGMxMGExMjIzLThkYWYtNGYzNS1hODljLWRjNTI4YjZjZmQwMgBGAAAAAACwxuHFVVuTRp9oUfgBwL8dBwBJttsErGCATZmSkeLkC5_YAAAAAAENAABJttsErGCATZmSkeLkC5_YAAFfJwL-AAA=");
		lookMailConnector.updateCalendarMetadata(emailFlagsInfo);
	}
	@Test
	void testDataRemoveal() {
		connectorService.getEmailInfoRepoImpl().getNotProcessedEmailsByDuplicateSourceID("64d24af80f96f44335f72cf4");
	}

	@Test
	void testCloudReaDding() {
		VendorOAuthCredential credentials = new VendorOAuthCredential();
		credentials.setAccessToken("ya29.a0AfB_byC9G9-JxYXCfUSapmHFFFhzoice_i5JZv_M0BszRYo2-spN43evUmK20G1d0Hamvl45y5jE5WLDM5zE11n0UqMXJ9HVOLRNH6a4SjOPpnImW2qt7OEwcOTpvEw4tlEl6ptUZcLwn8LI7DAGv7bO2EtyZo0h1mmdEGV-K6OZjNwF6e2nchqISq3ZRN6gzqef7CuqeGzX9rTTqDE6e4PIzL1CNqoN9E6jDxCJSZtQHAqmBp08aCgYKATgSARISFQHGX2MipFUyXKMxue8n22haTckHxA0251");
		credentials.setRefreshToken("emailmigrationapi@emailmigration-377717.iam.gserviceaccount.com");
		credentials.setAdminEmail("alexandret@proconsupplies.com");
		mailConnectorService.createVendor(CLOUD_NAME.GMAIL, credentials, "64c8ca416882854f9b89573e");

	}

	@Test
	void testEmailDestroyer() {
		customScript.deleteMailsBasedOnWorkSpceId("64d24af80f96f44335f72cf4", Arrays.asList("64d24af80f96f44335f72cec","64d24af80f96f44335f72cee","64d24af80f96f44335f72cf0","64d24af80f96f44335f72cf4","64d24af80f96f44335f72cf6"), false);
	}

	@Test
	void sendSimpleMail() {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setCloudId("64d352bde5b5e80198fecc67");
		//emailFlagsInfo.setOrganizer("harry@gajha.com");
		//		emailFlagsInfo.setFromName(info.getFromName());
		//		emailFlagsInfo.setSubject(info.getSubject());
		//		emailFlagsInfo.setAttendees(info.getAttendees());
		//		emailFlagsInfo.setStartTime(info.getStartTime());
		//		emailFlagsInfo.setEndTime(info.getEndTime());
		emailFlagsInfo.setId("Calender");
		lookMailConnector.sendEmail(emailFlagsInfo);
	}
	@Test
	void testGetCalendarEvent() {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId("650068a4d8c34d6d598c594c");
		List<CalenderInfo> infos = gMailConnector.getCalendarEvents(emailFlagsInfo);
		List<CalenderInfo> list = new ArrayList<>();
		for(CalenderInfo info : infos) {
			CalenderInfo _info = connectorService.getCalendarInfoRepoImpl().findBySourceId("64df7ebc9a765c2b59bc48fd", info.getSourceId());
			if(_info!=null && _info.getProcessStatus().equals(CalenderInfo.PROCESS.CONFLICT) && _info.getRecurrenceType()==null) {
				_info.setRecurrenceType(info.getRecurrenceType());
				list.add(_info);
				System.out.println("---"+info.getSourceId()+"************info--");
			}else {
				System.out.println("---"+info.getSourceId()+"--no info--");
			}
			//lookMailConnector.createCalenderEvent(emailFlagsInfo);
		}
		connectorService.getCalendarInfoRepoImpl().saveCalendars(list);
		list.clear();
	}

	@Test
	void testGmailData() {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId("653a532e83e0ac5c1a475335");
		emailFlagsInfo.setId("0it0taho5vgd778nvliov0dncs");
		emailFlagsInfo.setCalendar("harry@testing.us");
		List<CalenderInfo> sourceInfos = gMailConnector.getEventInstances(emailFlagsInfo);
		System.out.println("**** Destination total *****"+sourceInfos.size());

	}	
	@Test
	void deleteData() {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setCloudId("64dd0f563be5ed67b284c1d9");
		emailFlagsInfo.setFolder("TRASH");
		Clouds cloud = connectorService.getCloudsRepoImpl().findOne("64dd0f563be5ed67b284c1d9");
		gMailConnector.deleteBatchEmails(cloud, emailFlagsInfo, false, Arrays.asList("189f63b36c5950c9" ,
				"189f63b37cbbe59c" , 
				"189e5a787ac5355c" , 
				"189e5a78775b0f9f" , 
				"189e5a7874fac080" , 
				"189e5a787aaced82" , 
				"189f6387b22d8c88" , 
				"189f6387a17fc048" , 
				"189e5727df7e3e1e" , 
				"189e5727d3414492" , 
				"189e5727da050ac8" ,
				"189e5727c0ac4ddd"));
	}

	@Test
	void testCreateOutlookCalendarEvent() {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId("657c2e2ba532181c873af4ed");
		emailFlagsInfo.setId("Calendar");
		emailFlagsInfo.setCalendar("Calendar");
		emailFlagsInfo.setFutureEvents(true);
		emailFlagsInfo.setStartTime(LocalDateTime.now().toString());
		emailFlagsInfo.setEndTime(emailFlagsInfo.getStartTime());
		List<CalenderInfo> infos = lookMailConnector.getCalendarEvents(emailFlagsInfo);
		for(CalenderInfo info : infos) {
			if(info.getRange()==null) {
				continue;
			}
			EventRangeUtils range = EventRangeUtils.setRangeForGmailAsSource(info.getRange(),info.getRecurrenceType());
			if(range.getEndDate()!=null) {
				if(LocalDate.now().isAfter(TimeUtils.convertTimeToLocalDate(range.getEndDate()))){
					System.out.println("past Event");
					//return true;
				}
			}else if(range.getLastOccurence()!=null) {
				if(LocalDate.now().isAfter(range.getLastOccurence())){
					System.out.println("past Event");
				}
			}
			else if( "noEnd".equals(range.getType())) {
				System.out.println("Future Event");
			}
		}
		Assertions.assertNotNull(infos);
	}

	@Test
	void testGroups() {
		Clouds cloud = connectorService.getCloudsRepoImpl().findOne("64f9efa99570e22a671af599");
		new GroupsSaveTask(cloud, connectorService.getCloudsRepoImpl(), mailServiceFactory).run();
	}

	@Test
	void testGoogleGroups() {
		mailMigrationMgmtService.checkTheEventsForSendDeleteEvents("64e7715f6235951d68636fc6");
	}


	@Test
	void getAccessToken() {
		String authCode ="0.AVAAENLmDZSsHUapNU9sEFI5pJ-2iwb6UgRBvJjaQQU8BA9QAJw.AgABAAIAAAAtyolDObpQQ5VtlI4uGjEPAgDs_wUA9P9jRkdzsNm-vaA8Azc8R6V0R1thsMjWVIR51ZiEJbn2NGbz9na0N4Y1r2s7kl0MdDLZfFkthCXsdKteHBNayAM8JitG3QJvuKPEwOWann7o3nmJkisUnqWl18e9Lq3kxVFh0B2GB7xtHpwhJM9CSMSdIIhAkePJFNQ0OdydDgPJGF7rxLmAmASUa72SgzRWJ7ylamh78s6avP41IgrEjn5NtMzqo-ZCRkc_rfpxihELz1-arnGlrYk53ReNSCZQZLb8hGoioZyHB72nhtQoCW7rGA3d1hE-F6G9JPuoYwEpErYLWMQuxlF0KrdBwyqJYPvraim2c_48XXoa97LiMJagdiAwDk76_OUmBIpzmvwFk0wlRqgePm7g4hvXxO4wR9-NcK-aoOWzFTle5_chhX-mz8vxdhJAmrbhZ__cln5IMIWU6aphwpJhXK4fVQMxnFPy-D9fsZ7n38cnFdGQfS-5yr9Q6_WsnN6OF2baP7k9GydumrVBi68nMkimR2s7aH80kCTh0YadEmX5Kn-3V-xGxYD2GYQWb-plp9E4bPa45GIDtyCiA02X86SkCimlaLqC9xWxXHiTrzK4FlyqbyKvZpqlwtLHwLeSkNKbxri4QWozIDQC8rz_uoB6sdCL_nf7Qp6y3nbelNuf6BBlcSSPF4Rf1r_0Ubdo8HHCCI_PVaokcRRioxC2n4aeV9-7y9b1__URKCx_jxFLsjxt2j1rXD5NQMmBi3SizZ60a4RPsTDbW9bgMfUms27MJG9OZm1DtetS9Si-5FS0v-mmVU2I_7_U29-f5I8xqveyXl4K8ryBxQmcXpLsnEeJ5wECSdfMOhFHeb66FWLQqzsHtUKqdBN9Eduz809a6Hks0Ive6hgTzcVCsxWf-Dgfbv8Vs02yF49JToZRckki7nDogMjGBIi9aURUJpQEBXPe-iPnlDN7Mjl8P52ROHHM2mn2nPlomihIWToyKuX9P3q6qyeA_lS93dO4vyYh6tc2Lh1JGScRKiNvzBRO2hY9QhAROzMppVUbJhf4Numzu7zvtmKKKtu7206iSa_9eqS-N3f5v-7u706cYlw11RNT_2bhcHlnXQpIt5KmSI_JfgJeJTVZk0JtApI1rFer45jm2DFUFjH0lhklv6OUcVWuAspRhrgLK1-Q3WcjUL66V2Ujpj1mJhZp_HjwxHw_x0VGsfjHd046_3RMN5u8Xskp9FRCx_DoEhkVr-RLxdKTbCdM2l2wZIhvj8MTmjrnWWcUBfXicIt06QEeZ3EhBGeWjQQ511RJeUiM1CiiBSBEUoE-HCEAuYpmJo1MihRvqNd4uTTMdCUVWDAGY099kVDjXG8GxtzYoANYAp0IsSye&state=OUTLOOK%606391999bc2dcc733b6c77f96%60https%3a%2f%2fstaging.testing.com%2f&session_state=e0d251be-0b33-475b-b3b1-3a39b1433e7b";
		mailMappingConnector.getAccessToken(CLOUD_NAME.OUTLOOK, "", authCode);
	}

	@Test
	void testEvent() {

		EventRangeUtils range = EventRangeUtils.setRangeForOutlookAsSource("endDate#2024-01-08#2023-12-15#sunday#[monday]#0#2","relativeMonthly");
		checkOldEvent(range);
	}
	
	private boolean checkOldEvent(EventRangeUtils range) {
		if(range.getEndDate()!=null) {
			if(LocalDate.now().isAfter(TimeUtils.convertTimeToLocalDate(range.getEndDate()))){
				return true;
			}
		}else if(range.getLastOccurence()!=null) {
			if(LocalDate.now().isAfter(range.getLastOccurence())){
				return true;
			}
		}
		else if( "noEnd".equals(range.getType())) {
			return false;
		}
		return false;
	}

	@Test
	public void testTimeStamp() {
		List<String> times = Arrays.asList("2023-09-28T10:30:00+05:30","2023-09-28T10:30:00-05:30");

		for(String time :times) {
			if(time.split("\\-").length>3) {
				time = time.substring(0, time.lastIndexOf("-"));
			}else {
				time = time.split("\\+")[0];
			}
			System.out.println(time);
		}
	}

	@Test
	void testEventInstances() {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		emailFlagsInfo.setCloudId("655749a27a260c4f2272fdc6");
		emailFlagsInfo.setCalendar("harry@testing.us");
		emailFlagsInfo.setId("02ob3btv0k8rqct26pg397qns3");
		List<CalenderInfo> rules = gMailConnector.getEventInstances(emailFlagsInfo);
	}




	@Test
	void tesEventInstances() {
		CalenderFlags emailFlagsInfo = new CalenderFlags();
		long count = 0;
		while(true) {
			emailFlagsInfo.setCloudId("653a532e83e0ac5c1a475331");
			emailFlagsInfo.setCalendar("harry@testing.us");
			emailFlagsInfo.setId("alex@testing.us");
			List<CalenderInfo> rules = gMailConnector.getCalendarEvents(emailFlagsInfo);
			if(rules.isEmpty()) {
				System.out.println("No Events");
				break;
			}
			for(CalenderInfo rule : rules) {
				try {
					count+=1;
					System.out.println(count);
					EmailFlagsInfo _emailFlagsInfo = new EmailFlagsInfo();
					_emailFlagsInfo.setFolder("Calendar");
					_emailFlagsInfo.setFrom("alex@gajha.com");
					_emailFlagsInfo.setId(rule.getId());
					_emailFlagsInfo.setCloudId("655749707a260c4f2272fd47");
					_emailFlagsInfo.setFrom(rule.getOrganizer());
					System.out.println(rule.getId()+"--"+rule.getStartTime());
					lookMailConnector.deleteEmails(_emailFlagsInfo, true);
				} catch (Exception e) {
				}
			}

		}
	}


	@Test
	void testRules() {

		List<String>ids = Arrays.asList(
				"656d8cff79608d0b775d1701"  
				);

		for(String cloudId :ids) {
			EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
			emailFlagsInfo.setCloudId(cloudId);
			emailFlagsInfo.setFolder("all");
			long count = 1;
			do{
				try {
					int pageSize = 20;
					int sizeOfFileFolderMetaData = 0;
					int sizeOfSubList = pageSize;
					List<EmailFlagsInfo> rules = lookMailConnector.getListOfMails(emailFlagsInfo);
					System.out.println("****FEtched Total****"+rules.size());
					boolean processFolders = true;
					if(rules.isEmpty()) {
						continue;
					}
					while(processFolders) {
						List<EmailFlagsInfo> infos = new ArrayList<EmailFlagsInfo>();;
						List<String>requests = new ArrayList<>();
						if(rules.size()>sizeOfSubList) {
							infos = rules.subList(sizeOfFileFolderMetaData, sizeOfSubList);
						}else {
							infos = rules.subList(sizeOfFileFolderMetaData, rules.size());
							processFolders = false;
						}
						System.out.println("****DELETED***"+count);
						count = count+infos.size();
						for(EmailFlagsInfo rule : infos) {
							try {
								if(requests.contains(rule.getId())) {
									continue;
								}
								requests.add(rule.getId());
							} catch (Exception e) {
							}
						}
						System.out.println("*********** re size&&&&&&&&&&&&"+requests.size()+"-"+cloudId);
						emailFlagsInfo.setCloudId(cloudId);
						lookMailConnector.createBatchRequest(emailFlagsInfo, requests);
						//					try {
						//					//	Thread.sleep(1000);
						//					} catch (InterruptedException e) {
						//					}
						sizeOfFileFolderMetaData = sizeOfSubList;
						sizeOfSubList = sizeOfSubList+pageSize;
					}
				} catch (MailMigrationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}while(emailFlagsInfo.getNextPageToken()!=null);
		}
	}

	@Test
	void testLargeCreationOfMails() {
		customScript.createMailsInCloud("64ae920cdd9631217b1a9b95", "65657149bfe3fb7188a4a5aa", 10000, "Hey There test Data", "INBOX", false,null);
	}

	@Test
	void testGetContacts() {
		ContactsFlagInfo flagInfo = new ContactsFlagInfo();
		flagInfo.setCloudId("653a532e83e0ac5c1a475335");
		//gMailConnector.getContactsList(flagInfo);
	}


	@Test
	void testEmailJobDetails() {
		updateDuplcateRecords("65537d1c7a0248029f1b8825", 0);
	}


	private void updateDuplcateRecords(String jobId,int skip) {

		List<Document> pipeline = Arrays.asList(
				new Document("$match", new Document("emailWorkSpaceId", "65537d1c7a0248029f1b8826")),
				new Document("$group", new Document("_id", new Document("sourceId", "$sourceId"))
						.append("uniqueIds", new Document("$addToSet", "$_id"))
						.append("count", new Document("$sum", 1))),
				new Document("$match", new Document("count", new Document("$gt", 2))),
				new Document("$project", new Document("idsToRemove", new Document("$slice", Arrays.asList("$uniqueIds", 1)))
						.append("_id", 0))
				);

		MongoCollection<Document> mongoCollection = mongoTemplate.getCollection("emailInfo");
		AggregateIterable<Document> output = mongoCollection.aggregate(pipeline).allowDiskUse(true);
		MongoCursor<?>result = output.cursor();

		while(result.hasNext()) {
			Document data = (Document)result.next();
			if(data.get("idsToRemove")!=null) {
				Gson gson = new Gson();
				Type listType = new TypeToken<ArrayList<String>>(){}.getType();
				ArrayList<String> arrayList = gson.fromJson(data.get("idsToRemove").toString(), listType);
				System.out.println("ArrayList: " + arrayList);
				//dbService.getCalendarInfoRepoImpl().updateDuplicateEvents(arrayList);
			}
		}
	}

	@Test
	public void testMailFolders() {
		connectorService.getEmailInfoRepoImpl().getAggregartedResultForFolder("6560a67e83fbdb7f563162bb", "drafts");
	}
	
	@Test
	public void createWorkSpaceBasedOnJob() {
		String userId = "64df7ebc9a765c2b59bc48fd";
		List<EmailWorkSpace> list = new ArrayList<>();
		List<EmailJobDetails>details = connectorService.getEmailJobRepoImpl().getEmailJobDetails(userId);
		for(EmailJobDetails emailJobDetails : details) {
			for(String ids : emailJobDetails.getWorkspaceId()) {
				EmailWorkSpace emailWorkSpace = new EmailWorkSpace();
				EmailWorkSpace _emailWorkSpace = connectorService.getWorkSpaceRepoImpl().findOne(ids);
				if(_emailWorkSpace!=null) {
					continue;
				}
				emailWorkSpace.setId(ids);
				emailWorkSpace.setUserId(userId);
				emailWorkSpace.setProcessStatus(PROCESS.PROCESSED);
				emailWorkSpace.setReportStatus(EmailWorkSpace.REPORT_STATUS.PROCESSED);
				emailWorkSpace.setJobId(emailJobDetails.getId());
				emailWorkSpace.setFromMailId(emailJobDetails.getFromMailId());
				Clouds fromCloud = connectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailJobDetails.getFromMailId());
				if(fromCloud!=null) {
					emailWorkSpace.setFromCloudId(fromCloud.getId());
				}
				Clouds toCloud = connectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailJobDetails.getToMailId()); 
				if(toCloud!=null) {
					emailWorkSpace.setToCloudId(toCloud.getId());
				}
				emailWorkSpace.setFromCloud(CLOUD_NAME.GMAIL);
				emailWorkSpace.setToCloud(CLOUD_NAME.GMAIL);
				emailWorkSpace.setToMailId(emailJobDetails.getToMailId());
				emailWorkSpace.setFromFolderId("/");
				emailWorkSpace.setToFolderId("/");
				connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
			}
			list.clear();
		}
	}
	
	@Test
	void testOutlookTime() {
		TimeUtils.convertOutlookTimeFormatWithOffset("2024-01-15T13:30:00.0000000", "America/Mexico_City");
	}
	

}
