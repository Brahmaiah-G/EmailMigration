package com.testing.mail.rest;
/**
 * @author BrahmaiahG
*/

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.management.LargeCsvCreator;
import com.testing.mail.connectors.management.MappedPairsTask;
import com.testing.mail.constants.Const;
import com.testing.mail.constants.ExceptionConstants;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailBatches;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.EmailJobDetails;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.repo.entities.MappedUsers;
import com.testing.mail.service.DBConnectorService;
import com.testing.mail.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * For handling user Services 
 * <p></p>
 * <b>Operations:</b> User Mapping,PermissionCache,clouds, CSV
*/

@RestController
@Slf4j
@RequestMapping("/user")
public class MailUserMgmtService {

	private static final char SEPARATOR = ',';
	private static final char DEFAULT_SEPARATOR = ',';
	private static final char DEFAULT_QUOTE = '"';
	private static final String PASSSTATUS = "PASS";
	private static final String CREATEDPASS = "CREATED-PASS";
	private static final String FAILSTATUS ="FAIL";
	private static final List<String> mailFolders=Arrays.asList("inbox","sent items","drafts","archive","junk mail","outbox");

	Random random = new Random();

	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MailServiceFactory factory;

	@GetMapping("/mailFolders/{cloudId}")
	public ResponseEntity<?> getUserMailFolders(@PathVariable("cloudId")String cloudId){
		log.warn("==Going for getting the mailFolders for the cloud=="+cloudId);
		Clouds cloud = dbConnectorService.getCloudsRepoImpl().findOne(cloudId);
		if(ObjectUtils.isEmpty(cloud) || StringUtils.isBlank(cloudId)) {
			return HttpUtils.BadRequest(ExceptionConstants.REQUIRED_MISSING);
		}
		EmailFlagsInfo flagsInfo = createFlagsFromCloud(cloud);
		List<EmailFlagsInfo> mailFolders = factory.getConnectorService(cloud.getCloudName()).getListOfMailFolders(flagsInfo);
		return HttpUtils.Ok(mailFolders);
	}

	@GetMapping("/clouds")
	public ResponseEntity<?> getAdminCloudsByUser(@RequestAttribute("userId")String userId){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().findAdmins(userId));
	}

	@PutMapping("/clouds/{cloudId}")
	public ResponseEntity<?> updateCloudPremigrationStatus(@RequestAttribute("userId")String userId,@PathVariable("cloudId")String cloudId){
		Clouds cloud = dbConnectorService.getCloudsRepoImpl().findOne(cloudId);
		if(cloud!=null) {
			cloud.setPreMigrationStatus(null);
			dbConnectorService.getCloudsRepoImpl().save(cloud);
		}
		EmailWorkSpace workSpace = dbConnectorService.getWorkSpaceRepoImpl().getPremigrationWorkSpace(cloud.getEmail(), cloud.getCloudName().name());
		if(workSpace!=null) {
			dbConnectorService.getEmailInfoRepoImpl().removeEmails(workSpace.getId());
			dbConnectorService.getCalendarInfoRepoImpl().removeCalendars(workSpace.getId());
			dbConnectorService.getWorkSpaceRepoImpl().removeOne(workSpace.getId());
		}
		return HttpUtils.Ok("Success");
	}
	
	/**
	 * Getting Members Based on AdminMemberId
	*/
	@GetMapping("/clouds/{adminMemberId}")
	public ResponseEntity<?> getCloudsByAdminMemberId(@PathVariable("adminMemberId")String adminMemberId,@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,	@RequestParam("pageSize")int pageSize){
		log.warn("==Getting the members for the adminClud=="+adminMemberId);
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().findCloudsByAdminWithPazination(userId, adminMemberId, pageSize, pageNo));
	}

	private EmailFlagsInfo createFlagsFromCloud(Clouds clouds) {
		EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
		emailFlagsInfo.setCloudId(clouds.getId());
		emailFlagsInfo.setUserId(clouds.getUserId());
		return emailFlagsInfo;
	}
	
	
	public long createAutoMapping(String from,String to,String userId)	{

		long count = dbConnectorService.getCloudsRepoImpl().countMappedUsersList(userId, from, to, false);
		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(from);
		if(count>0 && fromCloud.getTotal()>count) {
			return count;
		}
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(to);
		long permissions = dbConnectorService.getPermissionCacheRepoImpl().countPermissionsFromAdmin(fromCloud.getAdminCloudId(), toCloud.getAdminCloudId(), userId);

		Map<String,Clouds>fromClouds = new HashMap<>();
		Map<String,Clouds>toClouds = new HashMap<>();
		List<Clouds>sourceClouds = dbConnectorService.getCloudsRepoImpl().findCloudsByAdminWithOutPagination(userId, fromCloud.getAdminMemberId());
		List<Clouds>destClouds = dbConnectorService.getCloudsRepoImpl().findCloudsByAdminWithOutPagination(userId, toCloud.getAdminMemberId());
		if(!sourceClouds.isEmpty()) {
			sourceClouds.stream().forEach(cloud->{
				fromClouds.put(cloud.getEmail().split("@")[0], cloud);
			});
		}
		List<MappedUsers> mappedUsersList = new ArrayList<>();
		List<PermissionCache> mappedPermissions = new ArrayList<>();
		if(!destClouds.isEmpty()) {
			destClouds.stream().forEach(cloud->{
				toClouds.put(cloud.getEmail().split("@")[0], cloud);
			});
		}
		Iterator<String> itr = fromClouds.keySet().iterator();
		while(itr!=null && itr.hasNext()) {
			String email = itr.next();
			boolean matched = false;
			if(toClouds.containsKey(email)) {
				matched = true;
			}
			MappedUsers users = createMappedUSers(fromCloud,toCloud,fromClouds.get(email), toClouds.get(email), matched, userId, false);
			if(fromClouds.get(email)!=null && permissions<= 0) {
				PermissionCache cache = createPermissionCache(fromClouds.get(email), toClouds.get(email));
				mappedPermissions.add(cache);
			}
			if(fromClouds.get(email)!=null && toClouds.get(email)!=null) {
				EmailBatches emailBatches = dbConnectorService.getCloudsRepoImpl().getBatchPerCloud(userId, fromClouds.get(email).getId(), toClouds.get(email).getId());
				if(emailBatches!=null) {
					users.setBatchId(emailBatches.getBatchId());
					users.setBatchName(emailBatches.getBatchName());
				}
			}
			mappedUsersList.add(users);
			if(mappedUsersList.size()>20) {
				dbConnectorService.getCloudsRepoImpl().saveMappedUsers(mappedUsersList);
				mappedUsersList.clear();
			}
			if(mappedPermissions.size()>20) {
				dbConnectorService.getPermissionCacheRepoImpl().savePermissions(mappedPermissions);
				mappedPermissions.clear();
			}
		}
		if(!mappedUsersList.isEmpty()) {
			dbConnectorService.getCloudsRepoImpl().saveMappedUsers(mappedUsersList);
			mappedUsersList.clear();
		}
		if(!mappedPermissions.isEmpty()) {
			dbConnectorService.getPermissionCacheRepoImpl().savePermissions(mappedPermissions);
			mappedPermissions.clear();
		}
		return dbConnectorService.getCloudsRepoImpl().countMappedUsersList(userId, from, to, false);
	}

	@GetMapping("/mapping/{from}/{to}")
	public ResponseEntity<?> getMappedUsers(@PathVariable("from")String from,@PathVariable("to")String to,@RequestAttribute("userId")String userId,
			@RequestParam("pageNo") int pageNo,	@RequestParam("pageSize")int pageSize)	{
		if(StringUtils.isEmpty(from) || StringUtils.isEmpty(to)) {
			return HttpUtils.BadRequest("Required Fields are missing");
		}
		List<MappedUsers> mappedUsers = null;

		Clouds fromCloud = dbConnectorService.getCloudsRepoImpl().findOne(from);
		Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findOne(to);
		if(ObjectUtils.isEmpty(fromCloud) || ObjectUtils.isEmpty(toCloud)) {
			return HttpUtils.BadRequest("Clouds not found");
		}
		Clouds fromAdminCloud =dbConnectorService.getCloudsRepoImpl().findOne(fromCloud.getAdminCloudId());
		long total = dbConnectorService.getCloudsRepoImpl().countMappedUsersList(userId, fromCloud.getAdminCloudId(), toCloud.getAdminCloudId(), false);
		if(fromAdminCloud!=null && fromAdminCloud.getProvisioned()>total) {
			//Added for mapping a larger user instead of 1 by one taking more time soo added Thread creation
			ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 20, 10000L, TimeUnit.MILLISECONDS,
					new SynchronousQueue<>());
			tpe.execute(new MappedPairsTask(fromCloud, toCloud, userId, dbConnectorService));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		total = dbConnectorService.getCloudsRepoImpl().countMappedUsersList(userId, fromCloud.getAdminCloudId(), toCloud.getAdminCloudId(), false);
		
		mappedUsers =  dbConnectorService.getCloudsRepoImpl().getMappedUsersList(userId, from, to, false, pageNo, pageSize);
		if(mappedUsers!=null &&  !mappedUsers.isEmpty()) {
			return HttpUtils.Ok(mappedUsers,total);
		}
		return null;
	}

	private MappedUsers createMappedUSers(Clouds fromAdminCloud,Clouds toAdmin,Clouds fromCloud,Clouds toCloud,boolean matched,String userId,boolean csv) {
		MappedUsers mappedUsers = new MappedUsers();
		mappedUsers.setUserId(userId);
		mappedUsers.setCsv(csv);
		if(fromCloud!=null) {
			mappedUsers.setFromCloudId(fromCloud.getId());
			mappedUsers.setFromCloudName(fromCloud.getCloudName().name());
			mappedUsers.setFromMailId(fromCloud.getEmail());
		}
		mappedUsers.setFromAdminCloud(fromAdminCloud.getId());
		mappedUsers.setToAdminCloud(toAdmin.getId());
		if(toCloud!=null) {
			mappedUsers.setToCloudId(toCloud.getId());
			mappedUsers.setToCloudName(toCloud.getCloudName().name());
			mappedUsers.setToMailId(toCloud.getEmail());
		}
		if(fromCloud!=null && toCloud!=null) {
			mappedUsers.setMatched(matched);
		}
		return mappedUsers;
	}

	private PermissionCache createPermissionCache(Clouds fromCloud,Clouds toCloud) {
		PermissionCache permissionCache = new PermissionCache();
		permissionCache.setFromAdminCloud(fromCloud.getAdminCloudId());
		if(toCloud!=null) {
			permissionCache.setToAdminCloud(toCloud.getAdminCloudId());
			permissionCache.setToCloudId(toCloud.getId());
			permissionCache.setToMail(toCloud.getEmail());
		}
		permissionCache.setFromCloudId(fromCloud.getId());
		permissionCache.setFromMail(fromCloud.getEmail());
		permissionCache.setUserId(fromCloud.getUserId());
		return permissionCache;
	}


	@GetMapping("/jobs")
	public ResponseEntity<?> getEmailJobs(@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize){
		return HttpUtils.Ok(dbConnectorService.getEmailJobRepoImpl().getEmailJobDEtails(userId, pageSize, pageNo));
	}

	@GetMapping("/jobs/{jobId}")
	public ResponseEntity<?> getWorkSpacesByJob(@PathVariable("jobId")String jobId,@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize){
		return HttpUtils.Ok(dbConnectorService.getWorkSpaceRepoImpl().getWorkspacesByJob(jobId, pageSize, pageNo));
	}


	@PutMapping("/jobs/{jobId}")
	public ResponseEntity<?> updateJobDetails(@PathVariable("jobId")String jobId,@RequestAttribute("userId")String userId,EmailJobDetails emailJobDetails){
		EmailJobDetails emailJobDetails2 = dbConnectorService.getEmailJobRepoImpl().findOne(jobId);
		if(emailJobDetails2!=null) {
			emailJobDetails.setId(jobId);
			emailJobDetails2 = emailJobDetails;
			dbConnectorService.getEmailJobRepoImpl().save(emailJobDetails2);
		}
		return HttpUtils.Ok(emailJobDetails2);
	}

	@GetMapping("/workspaces/{workSpaceId}")
	public ResponseEntity<?> getEmailInfosByWorkSpace(@PathVariable("workSpaceId")String workSpaceId,@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize,@RequestParam("type")String type
			,@RequestParam(value="folder",defaultValue="false")boolean folder,@RequestParam(value="folder",defaultValue="false")boolean calendar){
		List emailInfos = null;
		if(StringUtils.isEmpty(type) || type.equalsIgnoreCase("all")) {
			emailInfos = dbConnectorService.getEmailInfoRepoImpl().findByWorkSpace(workSpaceId, pageSize, pageNo,folder);
		}else {
			emailInfos = dbConnectorService.getEmailInfoRepoImpl().findByWorkSpaceAndProcessStatus(workSpaceId, pageSize, pageNo,type, folder);
		}
		if(folder) {
			return  HttpUtils.Ok(dbConnectorService.getEmailFolderInfoRepoImpl().findByWorkSpaceId(workSpaceId));
		}
		if(emailInfos!=null && !emailInfos.isEmpty()) {
			emailInfos.forEach(info->{
				((EmailInfo) info).setHtmlBodyContent(null);
			});
		}
		return HttpUtils.Ok(emailInfos);
	}

	@GetMapping("/workspaces/{workSpaceId}/events")
	public ResponseEntity<?> getEventInfosByWorkSpace(@PathVariable("workSpaceId")String workSpaceId,@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize,
			@RequestParam(value="folder",defaultValue="false")boolean folder){
		List infos = new ArrayList<>();

		if(folder) {
			List<CalenderInfo> cInfo = dbConnectorService.getCalendarInfoRepoImpl().findByWorkSpaceCalendars(workSpaceId, pageSize, pageNo,folder);
			if(!cInfo.isEmpty()) {
				cInfo.forEach(info->{
					info.setHtmlBodyContent(null);
					infos.add(info);
				});
			}
		}else {
			List<EventsInfo> cInfo = dbConnectorService.getCalendarInfoRepoImpl().findByWorkSpace(workSpaceId, pageSize, pageNo,folder);
			if(!cInfo.isEmpty()) {
				cInfo.forEach(info->{
					info.setHtmlBodyContent(null);
					infos.add(info);
				});
			}
		}

		return HttpUtils.Ok(infos);
	}
	
	
	@DeleteMapping("/csv/{id}")
	public ResponseEntity<?> deleteSingleCsvMapping(@RequestAttribute("userId")String userId,@PathVariable("id")String csvId){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().deleteCsvMappingsById(csvId,userId));
	}
	
	@DeleteMapping("/csv/{id}/all")
	public ResponseEntity<?> deleteAllCsvMapping(@RequestAttribute("userId")String userId,@PathVariable("id")String csvId){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().deleteCsvMappings(userId,csvId));
	}
	
	@GetMapping("/csv/{id}")
	public ResponseEntity<?> getCsvBasedOnId(@RequestAttribute("userId")String userId,@PathVariable("id")int csvId){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().getMappedUsersByCsvId(userId,csvId));
	}
	
	@DeleteMapping("/csv/{srcCloud}/{dstCloud}")
	public ResponseEntity<?> deleteCsvMapping(@RequestAttribute("userId")String userId,@PathVariable("srcCloud")String sourceAdminCloudId,@PathVariable("dstCloud")String destAdminCloudId){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().deleteCsvMappingsByClouds(sourceAdminCloudId, destAdminCloudId, true, userId));
	}
	
	
	@DeleteMapping("/mapping/{srcCloud}/{dstCloud}")
	public ResponseEntity<?> deleteUsersMapping(@RequestAttribute("userId")String userId,@PathVariable("srcCloud")String sourceAdminCloudId,@PathVariable("dstCloud")String destAdminCloudId,@RequestParam("csv")boolean csv){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().deleteMappingUsersByAdmin(userId, sourceAdminCloudId, destAdminCloudId, csv));
	} 
	
	
	@GetMapping("/csv/{from}/{to}")
	public ResponseEntity<?> getUserMappings(@PathVariable("from")String from,@PathVariable("to")String to,@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().getMappedUsersList(userId, from, to, true, pageNo, pageSize));
	}
	
	@GetMapping("/mapping/all")
	public ResponseEntity<?> getUserMappings(@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize,@RequestParam("csv")boolean csv){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().getAllMappedUsersList(userId, csv, pageNo, pageSize));
	}
	
	@GetMapping(value="/csv/validate/{id}", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE} )
	public ResponseEntity<?> downloadValidateCsv(@RequestAttribute("userId")String userId,@PathVariable("id")int csvId) throws FileNotFoundException, JsonProcessingException{
		// For Downloading CSV based on id with sourceNot Verified ones
		if(ObjectUtils.isEmpty(csvId)) {
			return HttpUtils.BadRequest("CSV Id is mandatory");
		}
		FileWriter writer = null;
		File file = null;
		List<MappedUsers> mappedUsers = dbConnectorService.getCloudsRepoImpl().getMappedUsersByCsvId(userId,csvId);
		
		if(ObjectUtils.isEmpty(mappedUsers)) {
			return HttpUtils.Ok("NO CSV's to Validate");
		}
		try {
			file =  File.createTempFile("emailCsv"+csvId, ".csv");
		} catch (IOException e) {
			log.error("===Exception while creating a tempFile for Csv Report==="+ExceptionUtils.getStackTrace(e));
		}
		try {
			writer = new FileWriter(file);
			appendHeadersForCsv(writer);
			if(mappedUsers!=null && !mappedUsers.isEmpty()) {
				for(MappedUsers users : mappedUsers) {
					String destMsg = "Pass";
					String srcMsg = "Pass";
					if(!users.isValid()) {
						srcMsg = users.getSourceErrorDesc();
						destMsg = users.getDestErrorDesc();
					}
					writer.append(StringEscapeUtils.escapeCsv(users.getFromMailId()));
					writer.append(",");
					writer.append(StringEscapeUtils.escapeCsv(users.getToMailId()));
					writer.append(",");
					writer.append(StringEscapeUtils.escapeCsv(srcMsg));
					writer.append(",");
					writer.append(StringEscapeUtils.escapeCsv(destMsg));
					writer.append(",");
					writer.append("\n");
				}
				
				writer.flush();
			}
			}catch(Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return HttpUtils.buildStreamOutResponse(new FileInputStream(file), "emailCsvmapping.csv");
	}
	
	/**
	 * Need To add Async Call for downloading the mapped pairs if users increases then it will take more time to do
	 */
	@GetMapping(value="/mapping/download/{srcId}/{dstId}", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE} )
	public ResponseEntity<?> downloadMappedUsers(@RequestAttribute("userId")String userId,@PathVariable("srcId")String srcId,@PathVariable("dstId")String destId) throws FileNotFoundException, JsonProcessingException{
		// For Downloading CSV based on id with sourceNot Verified ones
		
		FileWriter writer = null;
		File file = null;
		List<MappedUsers> mappedUsers = dbConnectorService.getCloudsRepoImpl().getMappedUsersList(userId, srcId, destId, false);
		if(ObjectUtils.isEmpty(mappedUsers)) {
			return HttpUtils.Ok("NO Mapping pairs to Validate");
		}
		try {
			file =  File.createTempFile("mappingCsv"+srcId, ".csv");
		} catch (IOException e) {
			log.warn("===Exception while creating a tempFile for Csv Report==="+ExceptionUtils.getStackTrace(e));
		}
		try {
			writer = new FileWriter(file);
			appendHeadersForMapping(writer);
			if(mappedUsers!=null && !mappedUsers.isEmpty()) {
				for(MappedUsers users : mappedUsers) {
					if(users!=null && !users.isCsv()) {
						writer.append(StringEscapeUtils.escapeCsv(users.getFromMailId()));
						writer.append(",");
						writer.append(StringEscapeUtils.escapeCsv(users.getToMailId()==null?"":users.getToMailId()));
						writer.append(",");
						writer.append("\n");
					}
				}
				writer.flush();
			}
		}catch(Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return HttpUtils.buildStreamOutResponse(new FileInputStream(file), "emailUserMapping.csv");
	}
	
	
	
	
	
	
	private void appendHeadersForCsv(FileWriter writer) throws IOException {
		writer.append("Source Email Address" +SEPARATOR+ "Destination Email Address" +SEPARATOR+
				"Source Email Validation"+SEPARATOR+ "Destination Email Validation");
		writer.append("\n");
	}
	
	private void appendHeadersForCsvUsers(FileWriter writer) throws IOException {
		writer.append("S.No" +SEPARATOR+ "Name" +SEPARATOR+
				"Email Address"+SEPARATOR+"Mailbox Size"+SEPARATOR+ "Mailbox Status");
		writer.append("\n");
	}
	
	private void appendHeadersForMapping(FileWriter writer) throws IOException {
		writer.append("Source Email Address"+SEPARATOR+ "Destination Email Address");
		writer.append("\n");
	}
	
	
	
	@PostMapping("/csv/{srcAdmin}/{dstAdmin}")
	public ResponseEntity<?> mapUsersBasedOnCsv(@RequestAttribute("userId")String userId,InputStream csvFile,@PathVariable("srcAdmin")String sourceAdminCloudId,@PathVariable("dstAdmin")String destAdminCloudId){
		log.warn("==Entered for csv uploading for user==="+userId);
		int csvId = random.nextInt(1000) ; 
		try {
			if (csvFile == null){
				return HttpUtils.BadRequest(HttpUtils.ERROR_REASONS.NO_CONTETNT_TO_PROCEED.name());
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();


			byte[] buffer = new byte[1024];
			int len;
			while ((len = csvFile.read(buffer)) > -1 ) {
				baos.write(buffer, 0, len);
			}
			baos.flush();
			//----------------------------------------------------------------------------------------------------------------------------------------
			// Read number  of lines in file if it greater than 100 return error without any process
			//----------------------------------------------------------------------------------------------------------------------------------------
			InputStream inputStream1 = new ByteArrayInputStream(baos.toByteArray());
			BufferedReader br1 = new BufferedReader(new InputStreamReader(inputStream1));
			String lineFromInput = br1.readLine();
			int totalLine = 0 ;
			while ((lineFromInput = br1.readLine()) != null && !lineFromInput.isEmpty()) {
				totalLine++;
				if (totalLine > 100) {
					ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 20, 10000L, TimeUnit.MILLISECONDS,
							new SynchronousQueue<>());
					tpe.execute(new LargeCsvCreator(dbConnectorService, new ByteArrayInputStream(baos.toByteArray()), userId, sourceAdminCloudId, destAdminCloudId));
					return HttpUtils.Ok("Large Csv Initiated");
				}
			}

			InputStream inputStream = new ByteArrayInputStream(baos.toByteArray()); 

			Clouds sourceAdminCloud = dbConnectorService.getCloudsRepoImpl().findOne(sourceAdminCloudId);
			if (sourceAdminCloud == null) {
				return HttpUtils.BadRequest("Invalid  Source Cloud Id, Provide valid cloud Id");
			}

			Clouds destAdminCloud = dbConnectorService.getCloudsRepoImpl().findOne(destAdminCloudId);
			if (destAdminCloud == null) {
				return HttpUtils.BadRequest("Invalid  Destination Cloud Id, Provide valid cloud Id");
			}
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			Map<String,String>mappingPath = new HashMap<>();
			List<MappedUsers> mapping = new ArrayList<>();
			String readLine = bufferedReader.readLine();
			while(StringUtils.isNotBlank(readLine) && readLine!=null) {
				log.warn("===CSV Read line==="+readLine+"===for user==="+userId);
				try {
					if(readLine.contains("Source Email Address")) {
						readLine = bufferedReader.readLine();
						continue;
					}
					List<String> fields = parseLine(readLine.trim());
					boolean srcFailed = false;
					boolean destFailed = false;
					if(fields!=null && StringUtils.isEmpty(fields.get(0).trim())){
						srcFailed = true;
					}else if(fields!=null && StringUtils.isEmpty(fields.get(1).trim())) {
						destFailed = true;	
					}
					if(fields.size()==2 || fields.size()==4){
						try {
							if(mappingPath.containsKey(fields.get(0).trim())){
								String existingUser = mappingPath.get((fields.get(0).trim()));
								if(existingUser.equals(fields.get(1).trim())) {
									readLine = bufferedReader.readLine();
									continue;
								}
							}
							mappingPath.put(fields.get(0).trim(),fields.get(1).trim());
							Clouds sourceCloud =null;
							Clouds destCloud = null;
							boolean verify = false;
							if(!srcFailed) {
								sourceCloud = getCloudByPath(userId,fields.get(0).trim(),sourceAdminCloudId);
							}
							if(!destFailed) {
								destCloud = getCloudByPath(userId,fields.get(1).trim(),destAdminCloudId);
							}
							if(sourceCloud != null && destCloud != null){
								verify = true;
							}
							MappedUsers mappedUsers = createMappingCache(sourceCloud, destCloud, true, verify, fields.get(0).trim(), fields.get(1).trim(), csvId,userId);
							if(srcFailed) {
								mappedUsers.setSourceErrorDesc("Source Email Address should not be blank");
							}
							if(destFailed) {
								mappedUsers.setDestErrorDesc("Destination Email Address should not be blank");
							}
							mapping.add(mappedUsers);
						} catch (Exception e) {
							log.info(ExceptionUtils.getStackTrace(e));
						}
					}
					readLine = bufferedReader.readLine();
					if(mapping.size()>20) {
						dbConnectorService.getCloudsRepoImpl().saveMappedUsers(mapping);
						mapping.clear();
					}
				} catch (Exception e) {
					readLine = bufferedReader.readLine();
					log.error(ExceptionUtils.getStackTrace(e));
				}
			}
			if(!mapping.isEmpty()) {
				dbConnectorService.getCloudsRepoImpl().saveMappedUsers(mapping);
				mapping.clear();
			}
		}catch(Exception e) {
			return HttpUtils.BadRequest(ExceptionUtils.getStackTrace(e));
		}
		return HttpUtils.Ok(getCsvBasedOnId(userId, csvId));
	}


	Clouds getCloudByPath(String emailId,String userId) {
		return dbConnectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailId);
	}

	Clouds getCloudByPath(String userId,String emailId,String adminMemberId) {
		return dbConnectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(userId, emailId, adminMemberId);
	}

	public static List<String> parseLine(String cvsLine) {
		return parseLine(cvsLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
	}


	public static List<String> parseLine(String cvsLine, char separators, char customQuote) {

		List<String> result = new ArrayList<>();

		//if empty, return!
		if (cvsLine == null || cvsLine.isEmpty()) {
			return result;
		}

		StringBuilder curVal = new StringBuilder();
		boolean inQuotes = false;
		boolean startCollectChar = false;
		boolean doubleQuotesInColumn = false;

		char[] charsArray = cvsLine.toCharArray();

		for (char eachChar : charsArray) {

			if (inQuotes) {
				startCollectChar = true;
				if (eachChar == customQuote) {
					inQuotes = false;
					doubleQuotesInColumn = false;
				} else {

					if (eachChar == '\"') {
						if (!doubleQuotesInColumn) {
							curVal.append(eachChar);
							doubleQuotesInColumn = true;
						}
					} else {
						curVal.append(eachChar);
					}

				}
			} else {
				if (eachChar == customQuote) {

					inQuotes = true;

					if (charsArray[0] != '"' && customQuote == '\"') {
						curVal.append('"');
					}

					//double quotes in column will hit this!
					if (startCollectChar) {
						curVal.append('"');
					}

				} else if (eachChar == separators) {

					result.add(curVal.toString());

					curVal = new StringBuilder();
					startCollectChar = false;

				} else if (eachChar == '\r') {
					//ignore LF characters
					continue;
				} else if (eachChar == '\n') {
					//the end, break!
					break;
				} else {
					curVal.append(eachChar);
				}
			}

		}

		result.add(curVal.toString());

		return result;
	}


	MappedUsers createMappingCache(Clouds sourceCloud,Clouds destClouds,boolean csv,boolean matched,String fromMailId,String destMailId,int csvId,String userId) {
		MappedUsers mappedUsers = new MappedUsers();
		String sourceMsg = null;
		String destMsg = null;
		if(sourceCloud!=null && sourceCloud.isActive()) {
			mappedUsers.setFromAdminCloud(sourceCloud.getAdminCloudId());
			mappedUsers.setFromCloudName(sourceCloud.getCloudName().name());
			mappedUsers.setFromCloudId(sourceCloud.getId());
			mappedUsers.setFromMailId(sourceCloud.getEmail());
			mappedUsers.setSourceVerifiedUser(true);
			sourceMsg = "Pass";
		}else {
			mappedUsers.setSourceVerifiedUser(false);
			mappedUsers.setFromMailId(fromMailId);
			if(sourceCloud!=null) {
				sourceMsg = sourceCloud.getErrorDescription()==null?sourceCloud.getMailBoxStatus():sourceCloud.getErrorDescription();
			}else {
				sourceMsg = "This email address does not exist";
			}
		}
		if(destClouds!=null && destClouds.isActive()) {
			mappedUsers.setToAdminCloud(destClouds.getAdminCloudId());
			mappedUsers.setToCloudName(destClouds.getCloudName().name());
			mappedUsers.setToCloudId(destClouds.getId());
			mappedUsers.setToMailId(destClouds.getEmail());
			mappedUsers.setDestVerifiedUser(true);
			destMsg = "Pass";
		}else {
			mappedUsers.setDestVerifiedUser(false);
			mappedUsers.setToMailId(destMailId);
			if(destClouds!=null) {
				destMsg = destClouds.getErrorDescription()==null?destClouds.getMailBoxStatus():destClouds.getErrorDescription();
			}else {
				destMsg = "This email address does not exist";
			}
		}
		if(mappedUsers.isSourceVerifiedUser() && mappedUsers.isDestVerifiedUser()) {
			mappedUsers.setValid(true);
		}
		mappedUsers.setToMailFolder("/");
		mappedUsers.setFromMailFolder("/");
		mappedUsers.setSourceErrorDesc(sourceMsg);
		mappedUsers.setDestErrorDesc(destMsg);
		mappedUsers.setSourceVerified(true);
		mappedUsers.setDestVerified(true);
		mappedUsers.setCsv(csv);
		mappedUsers.setMatched(matched);
		mappedUsers.setUserId(userId);
		mappedUsers.setCsvId(csvId);
		return mappedUsers;

	}


	boolean verifyMatch(String sourceMail,String destMail) {
		try {
			if(StringUtils.isNotBlank(sourceMail) && StringUtils.isNotBlank(destMail)) {
				return sourceMail.split(Const.ATTHERATE)[0].equals(destMail.split(Const.ATTHERATE)[0]);
			}
		} catch (Exception e) {
		}
		return false;
	}
	
	
	@GetMapping("/batches/all")
	public ResponseEntity<?> getUserAllBatches(@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize){
		List<EmailBatches> batcheList = new ArrayList<>();
		List<EmailBatches> batches = dbConnectorService.getCloudsRepoImpl().getAllMBatches(userId,  pageNo, pageSize);
		List<String> batcNames = new ArrayList<>();
		batches.forEach(batch->{
			if(!batcNames.contains(batch.getBatchName())) {
				batcNames.add(batch.getBatchName());
				batcheList.add(batch);
			}
		});
		return HttpUtils.Ok(batcheList);
	}
	
	
	@GetMapping("/batches/{id}")
	public ResponseEntity<?> getUserBatchById(@RequestAttribute("userId")String userId,@PathVariable("id")int id){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().getBatchesById(userId, id));
	}
	
	@GetMapping("/batches/{srcCloud}/{dstCloud}")
	public ResponseEntity<?> getUserBatchesPerAdmin(@PathVariable("srcCloud")String from,@PathVariable("dstCloud")String to,@RequestAttribute("userId")String userId,
			@RequestParam("pageNo") int pageNo,	@RequestParam("pageSize")int pageSize){
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().getBatchesPerCloud(userId, from, to, pageNo, pageSize));
	}
	
	/**
	 * Getting User PermissionCache Based on FromAdmin and ToAdmin
	*/
	@GetMapping("/cache/{srcCloud}/{dstCloud}")
	public ResponseEntity<?> getUserAllPermissionCache(@PathVariable("srcCloud")String from,@PathVariable("dstCloud")String to,@RequestAttribute("userId")String userId,@RequestParam("pageNo")int pageNo,@RequestParam("pageSize")int pageSize){
		List<PermissionCache> cache = new ArrayList<>();
		cache = dbConnectorService.getPermissionCacheRepoImpl().getPermissionsFromAdmin(from, to, userId,pageNo,pageSize);
		if(cache.isEmpty()) {
			cache = dbConnectorService.getPermissionCacheRepoImpl().getPermissionsFromAdmin(from, userId,pageNo,pageSize);
		}
		return HttpUtils.Ok(cache);
	}
	
	/**
	 * Getting User PermissionCache Based on SourceAdmin
	*/
	@GetMapping("/cache/{srcCloud}")
	public ResponseEntity<?> getUserPermissionCache(@PathVariable("srcCloud")String from,@RequestAttribute("userId")String userId){
		return HttpUtils.Ok(dbConnectorService.getPermissionCacheRepoImpl().getPermissionCache(from));
	}     
	/**
	 * Updating User PermissionCache Based on Id
	*/
	@PutMapping("/cache/{srcCloud}")
	public ResponseEntity<?> updateUserPermissionCache(@PathVariable("srcCloud")String from,@RequestAttribute("userId")String userId,@RequestBody PermissionCache cache){
		PermissionCache permissionCache = dbConnectorService.getPermissionCacheRepoImpl().getPermissionCache(from);
		if(permissionCache!=null) {
			Clouds toCloud = dbConnectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(userId, cache.getToMail(),permissionCache.getToAdminCloud());
			permissionCache.setFromMail(cache.getFromMail());
			permissionCache.setToMail(cache.getToMail());
			permissionCache.setFromCloudId(permissionCache.getFromCloud());
			if(toCloud!=null) {
				permissionCache.setToCloudId(toCloud.getId());
			}
		}
		return permissionCache!=null? HttpUtils.Ok(dbConnectorService.getPermissionCacheRepoImpl().savePermissionCache(permissionCache)):HttpUtils.NotFound("Permission Cache not found");
	}

	/**
	 *Searching Emails based on search Param 
	 *@param adminCloudId - particular admin of the cloud(Ex :Gmail,Outlook)
	 *@param userId - userId
	 *@param q - search parameter
	 *@return  Cloud based on searching param
	 *
	*/
	@GetMapping("/search/{adminCloudId}")
	public ResponseEntity<?> searchCloudBasedOnName(@PathVariable("adminCloudId")String adminCloudId,@RequestAttribute("userId")String userId,@RequestParam("q")String q){
		log.warn("--Searching cloud based on user--"+q);
		return HttpUtils.Ok(dbConnectorService.getCloudsRepoImpl().getEmailBasedOnName(adminCloudId, userId, q));
	}
	
	@GetMapping("/pre/{fromAdmin}/dashboard")
	public ResponseEntity<?> getPremigrationDashBoard(@PathVariable("fromAdmin")String adminCloudId,@RequestAttribute("userId")String userId){
		return HttpUtils.Ok(dbConnectorService.getEmailInfoRepoImpl().getAggregartedResultForPremigration(userId, adminCloudId));
	}
	
	@GetMapping(value="/download/{cloudId}", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE} )
	public ResponseEntity<?> downloadUsers(@RequestAttribute("userId")String userId,@PathVariable("cloudId")String srcId) throws FileNotFoundException, JsonProcessingException{
		// For Downloading CSV based on id with sourceNot Verified ones
		
		FileWriter writer = null;
		File file = null;
		int pageSize = 100;
		int pageNo = 0;
		List<Clouds> mappedUsers = null;
		List<Clouds> totalUSers = new ArrayList<>();
		while(true) {
			mappedUsers = dbConnectorService.getCloudsRepoImpl().findCloudsByAdminWithPazination(userId, srcId, pageSize, pageNo);
			if(mappedUsers.isEmpty()) {
				break;
			}
			totalUSers.addAll(mappedUsers);
			pageNo = pageNo+pageSize;
		}
		
		try {
			file =  File.createTempFile("Users"+srcId, ".csv");
		} catch (IOException e) {
			log.warn("===Exception while creating a tempFile for Csv Report==="+ExceptionUtils.getStackTrace(e));
		}
		try {
			int no =1;
			writer = new FileWriter(file);
			appendHeadersForCsvUsers(writer);
			if(totalUSers!=null && !totalUSers.isEmpty()) {
				for(Clouds users : totalUSers) {
					if(users!=null ) {
						writer.append(StringEscapeUtils.escapeCsv(""+no));
						writer.append(",");
						writer.append(StringEscapeUtils.escapeCsv(users.getName()));
						writer.append(",");
						writer.append(StringEscapeUtils.escapeCsv(users.getEmail()));
						writer.append(",");
						writer.append(StringEscapeUtils.escapeCsv(users.getQuotaUsed()== null ?"0 Bytes":users.getQuotaUsed()));
						writer.append(",");
						writer.append(StringEscapeUtils.escapeCsv(users.isActive()?"Active":"In-Active"));
						writer.append(",");
						writer.append("\n");
						no= no+1;
					}
				}
			}
			writer.flush();
		}catch(Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return HttpUtils.buildStreamOutResponse(new FileInputStream(file), "emailUsers.csv");
	}
	
	
	//dbConnectorService.getCloudsRepoImpl().findCloudsByAdminWithPazination(userId, adminMemberId, pageSize, pageNo)
	
}
