package com.testing.mail.connectors.management;

/**
 * @author BrahmaiahG
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailBatches;
import com.testing.mail.repo.entities.MappedUsers;
import com.testing.mail.rest.MailUserMgmtService;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;




@Slf4j
public class MappedPairsTask implements Runnable {

	Clouds fromCloud;
	Clouds toCloud;
	String userId;
	DBConnectorService dbConnectorService;
	
	
	
	
	public MappedPairsTask(Clouds fromCloud, Clouds toCloud, String userId,
			DBConnectorService dbConnectorService) {
		this.fromCloud = fromCloud;
		this.toCloud = toCloud;
		this.userId = userId;
		this.dbConnectorService = dbConnectorService;
	}

	@Override
	public void run() {
		log.warn("==MappingThread=="+getClass().getSimpleName()+"--"+userId+"--From admin Id--"+fromCloud.getId()+"--To Admin Id--"+toCloud.getId());
		createAutoMapping(fromCloud, toCloud, userId);
	}
	
	/**
	 * For Creating auto mapping and PermissionCache when Permissions are not there
	 * then only creating the PermissionCache if(PermissionsCache>0) not creating
	 * cache Check before changing Calling from the Service class
	 * @param fromCloud : -From Cloud source Mail cloud for mapping 
	 * @param toCloud : -To Cloud destination Mail cloud for mapping
	 * @see {@link MailUserMgmtService}
	 * @see {@link Clouds}
	 * @return void
	 */
	public void createAutoMapping(Clouds fromCloud ,Clouds toCloud, String userId)	{
		Clouds fromAdmin = dbConnectorService.getCloudsRepoImpl().findOne(fromCloud.getAdminCloudId());
		Clouds toAdmin = dbConnectorService.getCloudsRepoImpl().findOne(toCloud.getAdminCloudId());
		
		boolean sucess = dbConnectorService.getCloudsRepoImpl().deleteMappingUsersByAdmin(userId, fromAdmin.getAdminCloudId(), toAdmin.getAdminCloudId(), false);
		log.warn("==Deleting of the Mapping is:"+sucess+"== foor the user:="+userId+"--From admin Id--"+fromCloud.getId()+"--To Admin Id--"+toCloud.getId());
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
			MappedUsers users = createMappedUSers(fromAdmin,toAdmin,fromClouds.get(email), toClouds.get(email), matched, userId, false);
			if(fromClouds.get(email)!=null  && permissions<= 0) {
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
			if(mappedUsersList.size()>100) {
				dbConnectorService.getCloudsRepoImpl().saveMappedUsers(mappedUsersList);
				mappedUsersList.clear();
			}
			if(mappedPermissions.size()>100) {
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
			mappedUsers.setMatched(true);
		}
		return mappedUsers;
	}

	private PermissionCache createPermissionCache(Clouds fromCloud,Clouds toCloud) {
		PermissionCache permissionCache = new PermissionCache();
		permissionCache.setFromAdminCloud(fromCloud.getAdminCloudId());
		permissionCache.setFromCloudId(fromCloud.getId());
		if(toCloud!=null) {
			permissionCache.setToAdminCloud(toCloud.getAdminCloudId());
			permissionCache.setToMail(toCloud.getEmail());
			permissionCache.setToCloudId(toCloud.getId());
			permissionCache.setToCloud(toCloud.getCloudName().name());
		}else {
			permissionCache.setToMail("");
		}
		permissionCache.setFromMail(fromCloud.getEmail());
		permissionCache.setUserId(fromCloud.getUserId());
		permissionCache.setFromCloud(fromCloud.getCloudName().name());
		return permissionCache;
	}

	
	
}
