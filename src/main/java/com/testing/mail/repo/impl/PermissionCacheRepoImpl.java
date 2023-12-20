package com.testing.mail.repo.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.testing.mail.constants.DBConstants;
import com.testing.mail.dao.entities.PermissionCache;
import com.testing.mail.repo.PermissionCacheRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class PermissionCacheRepoImpl implements PermissionCacheRepository {

	@Autowired
	MongoTemplate mongoTemplate;
	
	MongoTemplate getMongo() {
		return mongoTemplate;
	}
	
	
	@Override
	public void savePermissions(List<PermissionCache>cache) {
		cache.stream().forEach(user->
			mongoTemplate.save(user)
		);
	}
	
	
	@Override
	public PermissionCache savePermissionCache(PermissionCache permissionCache) {
		return mongoTemplate.save(permissionCache);
	}
	
	@Override
	public List<PermissionCache> getPermissionsFromAdmin(String fromAdmin,String toAdmin,String userId){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromAdmin).and("toAdminCloud").is(toAdmin));
		return mongoTemplate.find(query, PermissionCache.class);
	}
	
	@Override
	public List<PermissionCache> getPermissionsFromAdmin(String fromAdmin,String toAdmin,String userId,int pageNo,int pageSize){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromAdmin).and("toAdminCloud").is(toAdmin));
		return mongoTemplate.find(query.skip(pageNo).limit(pageSize), PermissionCache.class);
	}
	
	@Override
	public List<PermissionCache> getPermissionsFromAdmin(String fromAdmin,String userId,int pageNo,int pageSize){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromAdmin));
		return mongoTemplate.find(query.skip(pageNo).limit(pageSize), PermissionCache.class);
	}
	
	@Override
	public void deletePermissionCacheBasedOnAdmin(String fromAdmin){
		Query query = new Query(Criteria.where("fromAdminCloud").is(fromAdmin));
		 mongoTemplate.remove(query, PermissionCache.class);
	}
	
	
	@Override
	public long countPermissionsFromAdmin(String fromAdmin,String toAdmin,String userId){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromAdmin).and("toAdminCloud").is(toAdmin));
		return mongoTemplate.count(query, PermissionCache.class);
	}
	
	
	@Override
	public PermissionCache getPermissionsByCloud(String fromAdmin,String toAdmin,String userId,String fromMail,String toMail){
		Query query = new Query(Criteria.where(DBConstants.USERID).is(userId).and("fromAdminCloud").is(fromAdmin).and("toAdminCloud").is(toAdmin).and("fromMail").is(fromMail).and("toMail").is(toMail));
		return mongoTemplate.findOne(query, PermissionCache.class);
	}
	
	@Override
	public PermissionCache getPermissionCache(String id){
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.findOne(query, PermissionCache.class);
	}
	
}
