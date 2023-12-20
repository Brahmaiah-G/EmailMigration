package com.cloudfuze.mail.connectors.management;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.cloudfuze.mail.connectors.factory.MailServiceFactory;
import com.cloudfuze.mail.connectors.impl.OutLookMailConnector;
import com.cloudfuze.mail.constants.Const;
import com.cloudfuze.mail.dao.entities.EmailFlagsInfo;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace;
import com.cloudfuze.mail.repo.entities.EmailWorkSpace.PROCESS;
import com.cloudfuze.mail.repo.entities.MemberDetails;
import com.cloudfuze.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MailDraftsMigrationTask implements Runnable {

	private MailServiceFactory mailServiceFactory;
	private DBConnectorService connectorService; 
	EmailWorkSpace emailWorkSpace;
	List<EmailInfo> emailInfos;


	public MailDraftsMigrationTask(MailServiceFactory mailServiceFactory, DBConnectorService connectorService,
			EmailWorkSpace emailWorkSpace, List<EmailInfo> emailInfo) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = connectorService;
		this.emailWorkSpace = emailWorkSpace;
		this.emailInfos = emailInfo;
	}

	@Override
	public void run() {
		initiateMigration();
		log.info("***LEAVING THREAD***"+Thread.currentThread().getName());
		return ;
	}

	private void initiateMigration() {
		long count = 0;
		emailWorkSpace.setProcessStatus(PROCESS.IN_PROGRESS);
		emailWorkSpace = connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		Map<String, String> destMembers = getMemberDetails(emailWorkSpace.getUserId(), emailWorkSpace.getToAdminCloud());
		List<String>ids = new ArrayList<>();
		try {
			List<EmailInfo> infosList = new ArrayList<>();
			List<EmailFlagsInfo> emailFlagsInfos = new ArrayList<>();
			Map<String,EmailInfo> infoMap = new HashMap<>();

			for(EmailInfo emailInfo : emailInfos) {
				ids.add(emailInfo.getId());
				count = emailInfo.getRetryCount();
				log.info("===Entered for Draft Migration for=="+emailWorkSpace.getId()+"==fromFolder:="+emailInfo.getMailFolder()+"==Id:=="+emailInfo.getId()+"==workSpaceId=="+emailWorkSpace.getId()+"==Started ON=="+new Date(System.currentTimeMillis()));
				emailInfo.setJobId(emailWorkSpace.getJobId());
				EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
				emailFlagsInfo.setCopy(emailWorkSpace.isCopy());
				emailFlagsInfo.setAdminMemberId(emailWorkSpace.getFromCloudId());
				emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
				emailFlagsInfo.setUserId(emailWorkSpace.getUserId());
				if(!emailWorkSpace.isCopy() && destMembers.containsKey(emailInfo.getFromMail())) {
					emailInfo.setAdminDeleted(true);
				}
				if(emailWorkSpace.isDeltaMigration()) {
					emailFlagsInfo.setThreadId(emailInfo.getThreadId());
				}
				emailFlagsInfo.setDeltaThread(emailInfo.isDeltaThread());
				emailFlagsInfo.setFrom(emailInfo.getFromMail());
				emailFlagsInfo.setThreadId(emailInfo.getDestThreadId());
				emailFlagsInfo.setThread(emailInfo.getOrder()>0);
				emailFlagsInfo.setId(emailInfo.getId());
				emailFlagsInfo.setDestId(emailInfo.getDestId());
				if("sentitems".equalsIgnoreCase(emailInfo.getMailFolder())) {
					emailFlagsInfo.setFolder(emailInfo.getMailFolder());
				}else {
					emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
				}
				try {
					if(!emailInfo.isDeleted()){
						// added for custom folders as we can't fetch by name so going by id // issue for the drafts in this scenario for custom folders need to check in diff way
						//emailFlagsInfo.setFolder(emailInfo.getDestFolderPath());
						emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
						emailFlagsInfo.setId(emailInfo.getId());
						emailFlagsInfos.add(emailFlagsInfo);
						infoMap.put(emailInfo.getId(), emailInfo);
					}else {
						//For Deleting the mail from inbox directly moving to deletedItems
						emailFlagsInfo.setCloudId(emailWorkSpace.getToCloudId());
						String folder= emailInfo.getMovedFolder();

						emailFlagsInfo.setThreadId(emailInfo.getDestThreadId());
						emailFlagsInfo.setFolder(folder);
						emailFlagsInfo.setDestId(emailInfo.getDestId());

						EmailInfo created = mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()).moveEmails(emailFlagsInfo, emailInfo);
						created.setDeleted(true);
						emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);
						emailInfo.setDestId(created.getId());
						emailInfo.setDestThreadId(created.getDestThreadId());
						connectorService.getEmailInfoRepoImpl().save(emailInfo);
					}

				}catch(Exception e) {
					emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
					emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
					emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
					connectorService.getEmailInfoRepoImpl().save(emailInfo);
					return;
				}
			}
			EmailFlagsInfo flagsInfo = new EmailFlagsInfo();
			flagsInfo.setCloudId(emailWorkSpace.getToCloudId());

			List<EmailFlagsInfo> createdInfos= new ArrayList<>();
			try {
				createdInfos = ((OutLookMailConnector)mailServiceFactory.getConnectorService(Clouds.CLOUD_NAME.OUTLOOK)).createSendBatchRequest(emailFlagsInfos, flagsInfo);
			} catch (Exception e1) {
				connectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpace.getId(), EmailInfo.PROCESS.CONFLICT.name(), ExceptionUtils.getStackTrace(e1), ids,count);
				ids.clear();
				return;
			}
			for(EmailFlagsInfo info : createdInfos) {
				EmailInfo emailInfo = infoMap.get(info.getId());
				try {
					if(info!=null && !info.isConflict()) {
						emailInfo.setProcessStatus(EmailInfo.PROCESS.METADATA_STARTED);
						emailInfo.setErrorDescription("DRAFT_PROCESSED");
					}else {
						emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
						emailInfo.setRetryCount(emailInfo.getRetryCount()+1);
						emailInfo.setErrorDescription(info.getMessage()==null?"DRAFT_NOTPROCESSED":info.getMessage());
					}
					if(EmailInfo.PROCESS.DRAFT_MIGRATION_IN_PROGRESS.equals(emailInfo.getProcessStatus())){
						emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
					}
				} catch (Exception e) {
					log.error(ExceptionUtils.getStackTrace(e));
					emailInfo.setProcessStatus(EmailInfo.PROCESS.CONFLICT);
					emailInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
				}
				if(emailInfo!=null) {
					infosList.add(emailInfo);
				}
			}
			connectorService.getEmailInfoRepoImpl().save(infosList);
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			connectorService.getEmailInfoRepoImpl().findAndUpdateByWorkSpace(emailWorkSpace.getId(), EmailInfo.PROCESS.CONFLICT.name(), ExceptionUtils.getStackTrace(e), ids,count);
		}finally {
			connectorService.getWorkSpaceRepoImpl().save(emailWorkSpace);
		}

	}

	public Map<String,String>getMemberDetails(String userId,String adminCloudId){
		MemberDetails memberDetails = connectorService.getCloudsRepoImpl().findMemberDetails(userId, adminCloudId);
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

}


