package com.testing.mail.repo;

import java.util.List;

import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailBatches;
import com.testing.mail.repo.entities.GroupEmailDetails;
import com.testing.mail.repo.entities.MappedUsers;
import com.testing.mail.repo.entities.MemberDetails;

public interface CloudsRepository {

	public void save(Clouds clouds);
	public void save(List<Clouds> clouds);
	public Clouds findOne(String id);
	public List<Clouds>findByUser(String userId,String cloudName);
	public Clouds findAdmin(String userId,String adminMemberId);
	List<Clouds> findAdmins(String userId);
	List<Clouds> findCloudsByAdmin(String userId, String adminMemberId);
	List<Clouds> findCloudsByAdminWithPazination(String userId, String adminMemberId, int pageNo, int skip);
	Clouds findCloudsByEmailId(String userId, String emailId);
	void saveMappedUsers(List<MappedUsers> users);
	List<MappedUsers> getMappedUsersList(String userId, String sourceCloud, String destCloud, boolean csv, int pageNo,
			int pageSize);
	void removeCloudsByAdmin(String userId, String adminMemberId);
	Clouds findCloudsByEmailId(String userId, String emailId, String cloudName);
	Clouds findCloudsByEmailIdUsingAdmin(String userId, String emailId, String adminMemberId);
	boolean deleteCsvMappingsByClouds(String sourceCloudId, String destCloudId, boolean csv, String userId);
	boolean deleteMappingUsersByAdmin(String userId, String fromCloudId, String toCloudId, boolean csv);
	List<MappedUsers> getAllMappedUsersList(String userId, boolean csv, int pageNo, int pageSize);
	boolean deleteAllBatchesByAdmin(String userId, String fromCloudId, String toCloudId);
	EmailBatches getBatchPerCloud(String userId, String sourceCloud, String destCloud);
	List<EmailBatches> getBatchesPerCloud(String userId, String sourceCloud, String destCloud, int pageNo,
			int pageSize);
	List<EmailBatches> getAllMBatches(String userId, int pageNo, int pageSize);
	void saveBatches(List<EmailBatches> users);
	List<EmailBatches> getBatchesById(String userId, int id);
	List<Clouds> findCloudsByAdminWithOutPagination(String userId, String adminMemberId);
	long countMappedUsersList(String userId, String sourceCloud, String destCloud, boolean csv);
	List<MappedUsers> getMappedUsersList(String userId, String sourceCloud, String destCloud, boolean csv);
	List<MappedUsers> getMappedUsersByCsvId(String userId, int id);
	Clouds getEmailBasedOnName(String adminCloudId, String userId, String q);
	boolean deleteCsvMappingsById(String id, String userId);
	boolean deleteCsvMappings(String userId, String cloudId);
	boolean deleteBatchesByName(String userId, String fromCloudId, String toCloudId, String name);
	List<Clouds> findAllCloudsByAdmin(String userId, String adminCloudId);
	void saveMemberDetails(MemberDetails clouds);
	MemberDetails findMemberDetails(String userId, String adminCloudId);
	GroupEmailDetails findGroupDetails(String userId, String jobId, String email);
	void saveGroupDetails(List<GroupEmailDetails> clouds);
	boolean deleteMemberDetails(String adminCloudId);
	boolean deleteGroups(String adminCloudId);
}
