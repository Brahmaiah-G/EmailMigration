package com.cloudfuze.mail.repo.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.repo.EmailFolderInfoRepository;
import com.cloudfuze.mail.repo.entities.EmailFolderInfo;
import com.cloudfuze.mail.repo.entities.EmailInfo;
import com.cloudfuze.mail.repo.entities.PROCESS;

@Repository
public class EmailFolderInfoRepoImpl implements EmailFolderInfoRepository {

	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Override
	public EmailFolderInfo save(EmailFolderInfo folderInfo) {
		return mongoTemplate.save(folderInfo);
	}

	@Override
	public EmailFolderInfo findOne(String id) {
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.findOne(query, EmailFolderInfo.class);
	}

	@Override
	public List<EmailFolderInfo> findByWorkSpaceId(String emailWorkSpaceId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId));
		return mongoTemplate.find(query, EmailFolderInfo.class);
	}

	@Override
	public List<EmailFolderInfo> findByWorkSpaceIdUserId(String emailWorkSpaceId, String userId) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.USERID).is(userId));
		return mongoTemplate.find(query, EmailFolderInfo.class);
	}

	@Override
	public void saveAll(List<EmailFolderInfo> folderInfos) {
		folderInfos.forEach(this::save);
	}

	@Override
	public List<EmailFolderInfo> findByProcessStatus(String emailWorkSpaceId, PROCESS processStatus) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).is(processStatus));
		return mongoTemplate.find(query, EmailFolderInfo.class);
	}
	
	@Override
	public List<EmailFolderInfo> findByProcessStatus(String emailWorkSpaceId, List<PROCESS> processStatus) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.PROCESS_STATUS).in(processStatus));
		return mongoTemplate.find(query, EmailFolderInfo.class);
	}
	@Override
	public List<EmailFolderInfo> findConflictFolders(String workSpaceId,long retryCount) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.PROCESS_STATUS).is(PROCESS.CONFLICT.name()).and(DBConstants.RETRY).lt(retryCount)); 
		return mongoTemplate.find(query, EmailFolderInfo.class);
	}

	@Override
	 public EmailFolderInfo getParentFolderInfo(String workSpaceId,String sourceParent) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).orOperator(Criteria.where(DBConstants.SOURCE_ID).is(sourceParent),Criteria.where("mailFolder").is(sourceParent)).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.PROCESSED.name()));
		return mongoTemplate.findOne(query, EmailFolderInfo.class);
	}
	
	 public EmailFolderInfo getParentFolderInfoBySourceId(String workSpaceId,String sourceParent) {
			Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(workSpaceId).and(DBConstants.SOURCE_ID).is(sourceParent).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.PROCESSED.name()));
			return mongoTemplate.findOne(query, EmailFolderInfo.class);
		}
	
	@Override
	public void updateEmailInfoForDestChildFolder(String emailWorkSpaceId,String sourceThreadId,String destParent) {
		Query query = new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(emailWorkSpaceId).and(DBConstants.SOURCE_PARENT).is(sourceThreadId).and(DBConstants.PROCESS_STATUS).is(EmailInfo.PROCESS.PARENT_NOT_PROCESSED));
		Update update = new Update();
		update.set(DBConstants.PROCESS_STATUS, EmailInfo.PROCESS.NOT_STARTED);
		update.set(DBConstants.DEST_PARENT, destParent);
		mongoTemplate.updateMulti(query, update, EmailFolderInfo.class);
	}

	public long getInprogressFolders(String id) {
		Query query= new Query(Criteria.where(DBConstants.EMAILWORKSPACEID).is(id).and(DBConstants.PROCESS_STATUS).is(PROCESS.IN_PROGRESS.name()).and(DBConstants.FOLDER).is(true)); 
		return mongoTemplate.count(query, EmailFolderInfo.class);
	}

}
