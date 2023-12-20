package com.cloudfuze.mail.connectors.management;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.MappedUsers;
import com.cloudfuze.mail.service.DBConnectorService;
import com.cloudfuze.mail.utils.MappingUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LargeCsvCreator implements Runnable{

	DBConnectorService dbConnectorService;
	Random random = new Random();
	InputStream csvFile = null;
	String userId;
	String sourceAdminCloudId;
	String destAdminCloudId;
	
	private static final char SEPARATOR = ',';
	private static final char DEFAULT_SEPARATOR = ',';
	private static final char DEFAULT_QUOTE = '"';
	private static final String PASSSTATUS = "PASS";
	private static final String CREATEDPASS = "CREATED-PASS";
	private static final String FAILSTATUS ="FAIL";
	private static final List<String> mailFolders=new ArrayList(Arrays.asList(MappingUtils.MAIL_FOLDERS.values()));
	
	public LargeCsvCreator(DBConnectorService dbConnectorService, InputStream csvFile, String userId,
			String sourceAdminCloudId, String destAdminCloudId) {
		this.dbConnectorService = dbConnectorService;
		this.csvFile = csvFile;
		this.userId = userId;
		this.sourceAdminCloudId = sourceAdminCloudId;
		this.destAdminCloudId = destAdminCloudId;
	}
	public void createLargeCsv() {

		log.warn("==Entered for csv uploading for user==="+userId);
		int csvId = random.nextInt(1000) ;
		try {
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
			InputStream inputStream = new ByteArrayInputStream(baos.toByteArray()); 

			Clouds sourceAdminCloud = dbConnectorService.getCloudsRepoImpl().findOne(sourceAdminCloudId);
			Clouds destAdminCloud = dbConnectorService.getCloudsRepoImpl().findOne(destAdminCloudId);
			if(sourceAdminCloud==null || destAdminCloud==null) {
				return;
			}
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			Map<String,String>mappingPath = new HashMap<>();
			List<MappedUsers> mapping = new ArrayList<>();
			String readLine = bufferedReader.readLine();
			while(StringUtils.isNotBlank(readLine) && readLine!=null) {
				log.info("===CSV Read line==="+readLine+"===for user==="+userId);
				try {
					if(readLine.contains("Source Cloud")) {
						readLine = bufferedReader.readLine();
						continue;
					}
					List<String> fields = parseLine(readLine.trim());
					if(fields!=null && StringUtils.isEmpty(fields.get(0))){
						continue;
					}
					if(fields.size()==8 || fields.size()==4){
						try {
							if(mappingPath.containsKey((fields.get(0).trim()+":"+fields.get(1).trim()))) {
								String existingUser = mappingPath.get((fields.get(0).trim()+":"+fields.get(1).trim()));
								if(existingUser.equals(fields.get(2).trim()+":"+fields.get(3).trim())) {
									readLine = bufferedReader.readLine();
									continue;
								}
							}
							mappingPath.put(fields.get(0).trim()+":"+fields.get(1).trim(), fields.get(2).trim()+":"+fields.get(3).trim());
							if(fields.get(0).trim().isEmpty() || fields.get(2).trim().isEmpty()){
								break;
							}
							Clouds sourceCloud = getCloudByPath(userId,fields.get(0).trim(),sourceAdminCloudId);
							Clouds destCloud = getCloudByPath(userId,fields.get(2).trim(),destAdminCloudId);
							boolean verify = false;
							if(sourceCloud != null && destCloud != null){
								verify = true;
							}
							MappedUsers mappedUsers = createMappingCache(sourceCloud, destCloud, fields.get(1).trim(), fields.get(3).trim(), true, verify, fields.get(0).trim(), fields.get(2).trim(), csvId,userId);
							mapping.add(mappedUsers);
						} catch (Exception e) {
							log.warn(ExceptionUtils.getStackTrace(e));
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
			log.error(ExceptionUtils.getStackTrace(e));
		}

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
	
	Clouds getCloudByPath(String emailId,String userId) {
		return dbConnectorService.getCloudsRepoImpl().findCloudsByEmailId(userId, emailId);
	}

	Clouds getCloudByPath(String userId,String emailId,String adminMemberId) {
		return dbConnectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(userId, emailId, adminMemberId);
	}
	
	MappedUsers createMappingCache(Clouds sourceCloud,Clouds destClouds,String sourceFolder,String destFolder,boolean csv,boolean matched,String fromMailId,String destMailId,int csvId,String userId) {
		MappedUsers mappedUsers = new MappedUsers();
		String sourceMsg = null;
		String destMsg = null;
		if(sourceCloud!=null && sourceCloud.isActive()) {
			mappedUsers.setFromAdminCloud(sourceCloud.getAdminCloudId());
			mappedUsers.setFromCloudName(sourceCloud.getCloudName().name());
			mappedUsers.setFromCloudId(sourceCloud.getId());
			mappedUsers.setFromMailId(sourceCloud.getEmail());
			mappedUsers.setSourceVerifiedUser(true);
			sourceMsg = "Success";
		}else {
			mappedUsers.setSourceVerifiedUser(false);
			mappedUsers.setFromMailId(fromMailId);
			if(sourceCloud!=null) {
				sourceMsg = sourceCloud.getErrorDescription()==null?sourceCloud.getMailBoxStatus():sourceCloud.getErrorDescription();
			}else {
				sourceMsg = "UserNotFound";
			}
		}
		if(destClouds!=null && destClouds.isActive()) {
			mappedUsers.setToAdminCloud(destClouds.getAdminCloudId());
			mappedUsers.setToCloudName(destClouds.getCloudName().name());
			mappedUsers.setToCloudId(destClouds.getId());
			mappedUsers.setToMailId(destClouds.getEmail());
			mappedUsers.setDestVerifiedUser(true);
			destMsg = "Success";
		}else {
			mappedUsers.setDestVerifiedUser(false);
			mappedUsers.setToMailId(destMailId);
			if(destClouds!=null) {
				destMsg = destClouds.getErrorDescription()==null?destClouds.getMailBoxStatus():destClouds.getErrorDescription();
			}else {
				destMsg = "UserNotFound";
			}
		}
		if(mappedUsers.isSourceVerifiedUser() && mappedUsers.isDestVerifiedUser()) {
			mappedUsers.setValid(true);
		}
		mappedUsers.setToMailFolder(destFolder);
		mappedUsers.setFromMailFolder(sourceFolder);
		mappedUsers.setSourceErrorDesc(sourceMsg);
		mappedUsers.setDestErrorDesc(destMsg);
		mappedUsers.setSourceVerified(mailFolders.contains(sourceFolder.trim().toLowerCase()));
		mappedUsers.setDestVerified(mailFolders.contains(destFolder.trim().toLowerCase()));
		mappedUsers.setCsv(csv);
		mappedUsers.setMatched(matched);
		mappedUsers.setUserId(userId);
		mappedUsers.setCsvId(csvId);
		return mappedUsers;

	}
	@Override
	public void run() {
		createLargeCsv();
	}
	
	
}
