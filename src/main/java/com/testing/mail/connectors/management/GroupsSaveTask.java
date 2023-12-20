package com.testing.mail.connectors.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.GroupEmailDetails;
import com.testing.mail.repo.entities.PROCESS;
import com.testing.mail.repo.impl.CloudsRepoImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroupsSaveTask implements Runnable{

	@Override
	public void run() {
		fetchSourceEmailGroups();
	}
	private Clouds adminCloud;
	private CloudsRepoImpl cloudsRepoImpl;
	private MailServiceFactory mailServiceFactory;
	
	public GroupsSaveTask(Clouds adminCloud, CloudsRepoImpl cloudsRepoImpl, MailServiceFactory mailServiceFactory) {
		this.adminCloud = adminCloud;
		this.cloudsRepoImpl = cloudsRepoImpl;
		this.mailServiceFactory = mailServiceFactory;
	}

	private void fetchSourceEmailGroups() {
		PROCESS processStatus = PROCESS.NOT_PROCESSED;
		if(adminCloud!=null) {
			log.info("=Entered for Fetching groups from Email:-"+adminCloud.getEmail());
			try {
				processStatus = PROCESS.IN_PROGRESS;
				adminCloud.setGroupsStatus(processStatus);
				cloudsRepoImpl.save(adminCloud);
				List<GroupEmailDetails>groupEmailDetails = new ArrayList<>();
				List<UserGroups>userGroups = mailServiceFactory.getConnectorService(adminCloud.getCloudName()).getGroupEmailDetails(adminCloud.getId());
				adminCloud.setGroupsStatus(processStatus);
				cloudsRepoImpl.save(adminCloud);
				if(userGroups.isEmpty()){
					processStatus = PROCESS.PROCESSED;
				}else {
					userGroups.forEach(userGroup->
						groupEmailDetails.add(convertFromUserGroups(userGroup, adminCloud))
					);
				}
				cloudsRepoImpl.saveGroupDetails(groupEmailDetails);
				groupEmailDetails.clear();
				adminCloud.setGroupsStatus(processStatus);
				cloudsRepoImpl.save(adminCloud);
			} catch (Exception e) {
				processStatus = PROCESS.CONFLICT;
				log.error(ExceptionUtils.getStackTrace(e));
				adminCloud.setErrorDescription(ExceptionUtils.getStackTrace(e));
			}finally {
				adminCloud.setGroupsStatus(processStatus);
				cloudsRepoImpl.save(adminCloud);
			}
		}
	}

	private GroupEmailDetails convertFromUserGroups(UserGroups userGroups,Clouds clouds) {
		GroupEmailDetails groupEmailDetails = new GroupEmailDetails();
		groupEmailDetails.setEmail(userGroups.getEmail());
		groupEmailDetails.setGroupId(userGroups.getId());
		groupEmailDetails.setName(userGroups.getName());
		groupEmailDetails.setDescription(userGroups.getDescription());
		groupEmailDetails.setUserId(clouds.getUserId());
		groupEmailDetails.setAdminCloudId(clouds.getAdminCloudId());
		return groupEmailDetails;
	}
}
