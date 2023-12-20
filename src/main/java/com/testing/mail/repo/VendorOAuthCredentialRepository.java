package com.testing.mail.repo;

import com.testing.mail.repo.entities.VendorOAuthCredential;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

public interface VendorOAuthCredentialRepository /*extends MongoRepository<VendorOAuthCredential, String>*/{
	
	VendorOAuthCredential findByUserId(String userId);
	
	VendorOAuthCredential findByUserIdAndDomainNameAndVendor(String userId, String domain, CLOUD_NAME label, String vendorId);

	VendorOAuthCredential save(VendorOAuthCredential credentials);
	
	void delete(VendorOAuthCredential credential);

	VendorOAuthCredential findById(String id);

	VendorOAuthCredential removeOne(String id);
}
