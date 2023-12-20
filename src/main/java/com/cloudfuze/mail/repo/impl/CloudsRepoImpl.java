package com.cloudfuze.mail.repo.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.cloudfuze.mail.constants.DBConstants;
import com.cloudfuze.mail.repo.CloudsRepository;
import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailBatches;
import com.cloudfuze.mail.repo.entities.GroupEmailDetails;
import com.cloudfuze.mail.repo.entities.MappedUsers;
import com.cloudfuze.mail.repo.entities.MemberDetails;
import com.mongodb.client.result.DeleteResult;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CloudsRepoImpl implements CloudsRepository {

	@Autowired
	MongoTemplate mongoTemplate;

	MongoTemplate getMongo() {
		return mongoTemplate;
	}

	@Override
	public void save(Clouds clouds) {
		getMongo().save(clouds);

	}
	
	public void saveGroups(GroupEmailDetails clouds) {
		getMongo().save(clouds);
	}

	@Override
	public void save(List<Clouds> clouds) {
		clouds.stream().forEach(this::save);
	}

	@Override
	public Clouds findOne(String id) {
		Query query = new Query(Criteria.where(DBConstants.ID).is(id));
		return getMongo().findOne(query, Clouds.class);
	}

	@Override
	public List<Clouds> findByUser(String userId, String cloudName) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.CLOUDNAME).is(cloudName).and(DBConstants.ACTIVE).is(true));
		return getMongo().find(query, Clouds.class);
	}

	@Override
	public Clouds findAdmin(String adminMemberId, String userId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.ADMIN).is(true).orOperator(Criteria.where(DBConstants.ADMINCLOUDID).is(adminMemberId),Criteria.where(DBConstants.adminMemberId).is(adminMemberId)).and(DBConstants.ACTIVE).is(true));
		return getMongo().findOne(query, Clouds.class);
	}


	@Override
	public List<Clouds> findCloudsByAdmin(String userId, String adminMemberId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.adminMemberId).is(adminMemberId).and(DBConstants.ACTIVE).is(true));
		return getMongo().find(query, Clouds.class);
	}

	@Override
	public void removeCloudsByAdmin(String userId, String adminMemberId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.adminMemberId).is(adminMemberId));
		getMongo().remove(query, Clouds.class);
	}

	@Override
	public Clouds findCloudsByEmailId(String userId, String emailId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.EMAIL).is(emailId).and(DBConstants.ACTIVE).is(true));
		return getMongo().findOne(query, Clouds.class);
	}

	@Override
	public Clouds findCloudsByEmailIdUsingAdmin(String userId, String emailId,String adminMemberId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.EMAIL).is(emailId).and(DBConstants.ADMINCLOUDID).is(adminMemberId));
		return getMongo().findOne(query, Clouds.class);
	}

	@Override
	public Clouds findCloudsByEmailId(String userId, String emailId,String cloudName) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.EMAIL).is(emailId).and(DBConstants.CLOUDNAME).is(cloudName).and(DBConstants.ACTIVE).is(true));
		return getMongo().findOne(query, Clouds.class);
	}


	@Override
	public List<Clouds> findCloudsByAdminWithPazination(String userId, String adminMemberId,int pageNo,int skip) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).orOperator(Criteria.where(DBConstants.adminMemberId).is(adminMemberId),Criteria.where(DBConstants.ADMIN_EMAIL_ID).is(adminMemberId),Criteria.where(DBConstants.ADMINCLOUDID).is(adminMemberId)));
		return getMongo().find(query.limit(pageNo).skip(skip), Clouds.class);
	}

	@Override
	public Clouds getEmailBasedOnName(String adminCloudId,String userId,String q) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.ADMINCLOUDID).is(adminCloudId).and("email").regex(q, "i"));
		return mongoTemplate.findOne(query, Clouds.class);
	}


	@Override
	public List<Clouds> findCloudsByAdminWithOutPagination(String userId, String adminMemberId) {
		int pageNo = 0;
		int pageSize = 100;
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).orOperator(Criteria.where(DBConstants.adminMemberId).is(adminMemberId),Criteria.where(DBConstants.ADMIN_EMAIL_ID).is(adminMemberId)).and(DBConstants.ACTIVE).is(true));
		List<Clouds> total = new ArrayList<>();
		while(true) {
			List<Clouds> clouds = getMongo().find(query.limit(pageSize).skip(pageNo), Clouds.class);
			if(clouds!=null && !clouds.isEmpty()) {
				total.addAll(clouds);
			}else {
				break;
			}
			pageNo = pageSize+pageNo;
		}
		return total;
	}

	@Override
	public List<Clouds> findAdmins(String userId){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.ADMIN).is(true).and(DBConstants.ACTIVE).is(true));
		return getMongo().find(query, Clouds.class);
	}

	@Override
	public void saveMappedUsers(List<MappedUsers> users) {
		users.stream().forEach(user->{
			mongoTemplate.save(user);
		});
	}

	@Override
	public boolean deleteCsvMappingsById(String id,String userId) {
		Query query = new Query(Criteria.where("_id").is(id));
		DeleteResult result = mongoTemplate.remove(query, MappedUsers.class);
		return result.getDeletedCount()>0;
	}

	@Override
	public boolean deleteCsvMappings(String userId,String cloudId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(cloudId).and("csv").is(true));
		DeleteResult result = mongoTemplate.remove(query, MappedUsers.class);
		return result.getDeletedCount()>0;
	}

	@Override
	public List<MappedUsers> getMappedUsersByCsvId(String userId,int id) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("csvId").is(id));
		return mongoTemplate.find(query, MappedUsers.class);
	}

	@Override
	public boolean deleteCsvMappingsByClouds(String sourceCloudId,String destCloudId,boolean csv,String userId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromCloudId").is(sourceCloudId).and("toCloudId").is(destCloudId).and("csv").is(csv));
		DeleteResult result =mongoTemplate.remove(query, MappedUsers.class);
		if(result!=null) {
			return result.getDeletedCount()>0;
		}
		return false;
	}

	@Override
	public boolean deleteMappingUsersByAdmin(String userId,String fromCloudId,String toCloudId,boolean csv) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromCloudId).and("toAdminCloud").is(toCloudId).and("csv").is(csv));
		DeleteResult result =mongoTemplate.remove(query, MappedUsers.class);
		if(result!=null) {
			return result.getDeletedCount()>0;
		}
		return false;
	}


	@Override
	public List<MappedUsers> getMappedUsersList(String userId,String sourceCloud,String destCloud,boolean csv,int pageNo,int pageSize){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(sourceCloud).and("toAdminCloud").is(destCloud).and("csv").is(csv));
		Sort sort = Sort.by(Order.desc("matched"));
		return getMongo().find(query.limit(pageSize).skip(pageNo).with(sort), MappedUsers.class);
	}

	/**
	 * For getting mapped pairs based on CSV
	 */
	@Override
	public List<MappedUsers> getMappedUsersList(String userId,String sourceCloud,String destCloud,boolean csv){
		int pageNo = 0;
		int pageSize = 100;
		List<MappedUsers>usersList = new ArrayList<>();
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(sourceCloud).and("toAdminCloud").is(destCloud).and("csv").is(csv));
		while(true) {
			List<MappedUsers>users = getMongo().find(query.limit(pageSize).skip(pageNo), MappedUsers.class);
			if(!users.isEmpty()) {
				usersList.addAll(users);
				pageNo = pageNo+pageSize;
			}else {
				break;
			}
		}
		return usersList;
	}

	@Override
	public long countMappedUsersList(String userId,String sourceCloud,String destCloud,boolean csv){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(sourceCloud).and("toAdminCloud").is(destCloud).and("csv").is(csv));
		return getMongo().count(query, MappedUsers.class);
	}


	@Override
	public List<MappedUsers> getAllMappedUsersList(String userId,boolean csv,int pageNo,int pageSize){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("csv").is(csv));
		return getMongo().find(query.limit(pageSize).skip(pageNo), MappedUsers.class);
	}

	@Override
	public void saveBatches(List<EmailBatches> users) {
		users.stream().forEach(user->
		mongoTemplate.save(user)
				);
	}

	@Override
	public List<EmailBatches> getAllMBatches(String userId,int pageNo,int pageSize){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId));
		return getMongo().find(query.limit(pageSize).skip(pageNo), EmailBatches.class);
	}

	@Override
	public List<EmailBatches> getBatchesPerCloud(String userId,String sourceCloud,String destCloud,int pageNo,int pageSize){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(sourceCloud).and("toAdminCloud").is(destCloud));
		return getMongo().find(query.limit(pageSize).skip(pageNo), EmailBatches.class);
	}

	@Override
	public List<EmailBatches> getBatchesById(String userId,int id){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("batchId").is(id));
		return getMongo().find(query, EmailBatches.class);
	}

	@Override
	public EmailBatches getBatchPerCloud(String userId,String sourceCloud,String destCloud){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromCloudId").is(sourceCloud).and("toCloudId").is(destCloud));
		return getMongo().findOne(query, EmailBatches.class);
	}

	@Override
	public boolean deleteAllBatchesByAdmin(String userId,String fromCloudId,String toCloudId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromCloudId).and("toAdminCloud").is(toCloudId));
		DeleteResult result =mongoTemplate.remove(query, EmailBatches.class);
		if(result!=null) {
			return result.wasAcknowledged();
		}
		return false;
	}

	@Override
	public boolean deleteBatchesByName(String userId,String fromCloudId,String toCloudId,String name) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromCloudId).and("toAdminCloud").is(toCloudId).and("batchName").is(name));
		DeleteResult result =mongoTemplate.remove(query, EmailBatches.class);
		if(result!=null) {
			return result.wasAcknowledged();
		}
		return false;
	}

	@Override
	public List<Clouds> findAllCloudsByAdmin(String userId, String adminCloudId) {
		int pageNo = 0;
		int pageSize = 100;
		List<Clouds> clouds = new ArrayList<>();
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.ADMINCLOUDID).is(adminCloudId));
		while(true) {
			List<Clouds> _clouds = getMongo().find(query.limit(pageNo).skip(pageSize), Clouds.class);
			if(_clouds.isEmpty() || _clouds.size()<100) {
				break;
			}
			pageNo = pageNo+pageSize;
			clouds.addAll(_clouds);
		}
		return clouds;
	}


	@Override
	public void saveGroupDetails(List<GroupEmailDetails> clouds) {
		clouds.stream().forEach(cloud->{
			saveGroups(cloud);
		});
	}

	
	
	
	@Override
	public GroupEmailDetails findGroupDetails(String userId,String jobId,String email) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.ADMINCLOUDID).is(jobId).and(DBConstants.EMAIL).is(email));
		return getMongo().findOne(query, GroupEmailDetails.class);
	}

	@Override
	public MemberDetails findMemberDetails(String userId,String adminCloudId) {
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and(DBConstants.ADMINCLOUDID).is(adminCloudId));
		return getMongo().findOne(query, MemberDetails.class);
	}

	@Override
	public void saveMemberDetails(MemberDetails clouds) {
		getMongo().save(clouds);
	}
	@Override
	public boolean deleteMemberDetails(String adminCloudId) {
		Query query = new Query(Criteria.where(DBConstants.ADMINCLOUDID).is(adminCloudId));
		DeleteResult result =mongoTemplate.remove(query, MemberDetails.class);
		return result.getDeletedCount()>0;
	}
	@Override
	public boolean deleteGroups(String adminCloudId) {
		Query query = new Query(Criteria.where(DBConstants.ADMINCLOUDID).is(adminCloudId));
		DeleteResult result =mongoTemplate.remove(query, MemberDetails.class);
		return result.getDeletedCount()>0;
	}

}