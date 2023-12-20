package com.testing.mail.repo;

import java.util.List;

import com.testing.mail.dao.entities.PermissionCache;

public interface PermissionCacheRepository {

	void savePermissions(List<PermissionCache> cache);

	PermissionCache savePermissionCache(PermissionCache permissionCache);

	List<PermissionCache> getPermissionsFromAdmin(String fromAdmin, String toAdmin, String userId);

	List<PermissionCache> getPermissionsFromAdmin(String fromAdmin, String toAdmin, String userId, int pageNo,
			int pageSize);

	List<PermissionCache> getPermissionsFromAdmin(String fromAdmin, String userId, int pageNo, int pageSize);

	long countPermissionsFromAdmin(String fromAdmin, String toAdmin, String userId);

	PermissionCache getPermissionsByCloud(String fromAdmin, String toAdmin, String userId, String fromMail,
			String toMail);

	PermissionCache getPermissionCache(String id);

	void deletePermissionCacheBasedOnAdmin(String fromAdmin);

}
