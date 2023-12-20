package com.testing.mail.repo;

import java.util.List;

import com.testing.mail.repo.entities.PremigrationDetails;

public interface PremigrationRepository {

	public void save(PremigrationDetails premigrationDetails);
	public void save(List<PremigrationDetails>premigrationDetails);
	public PremigrationDetails findById(String id);
	public PremigrationDetails findByEmailOrCloud(String cloudId);
	public List<PremigrationDetails>findByUserId(String userId);
	List<PremigrationDetails> findByProcessStatus(List<String> processStauts);
	public PremigrationDetails findByWorkSpace(String workSpaceId);
}
