package com.testing.mail.utils;

import java.util.List;

import com.testing.mail.connectors.google.data.Group;
import com.testing.mail.connectors.microsoft.data.GroupValue;
import com.testing.mail.contacts.dao.ContactsFlagInfo;
import com.testing.mail.contacts.entities.Contacts;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.repo.entities.GroupEmailDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConvertionUtils {

	public static UserGroups convertGroupToGroupEmailDetails(Group group,List<String>members) {
		UserGroups groupEmailDetails = new UserGroups();
		groupEmailDetails.setName(group.getName());
		groupEmailDetails.setEmail(group.getEmail());
		groupEmailDetails.setId(group.getId());
		groupEmailDetails.setDescription(group.getDescription());
		groupEmailDetails.setMembers(members);
		groupEmailDetails.setMembersCount(group.getDirectMembersCount());
		return groupEmailDetails;
	}
	
	public static UserGroups convertGroupToGroupEmailDetails(GroupValue group,List<String>members) {
		UserGroups groupEmailDetails = new UserGroups();
		groupEmailDetails.setName(group.getDisplayName());
		groupEmailDetails.setEmail(group.getMail());
		groupEmailDetails.setId(group.getId());
		groupEmailDetails.setDescription(group.getDescription());
		groupEmailDetails.setMembers(members);
		groupEmailDetails.setMembersCount(members!=null ?members.size():0);
		groupEmailDetails.setCreatedTime(TimeUtils.convertStringToTime(group.getCreatedDateTime()));
		groupEmailDetails.setTypes(group.getGroupTypes());
		return groupEmailDetails;
	}
	
	
	
	public static UserGroups convertGroupEmailDetailsToUserGroup(GroupEmailDetails groupEmailDetails ) {
		UserGroups userGroup = new UserGroups();
		userGroup.setName(groupEmailDetails.getName());
		userGroup.setEmail(groupEmailDetails.getEmail());
		userGroup.setId(groupEmailDetails.getGroupId());
		userGroup.setDescription(groupEmailDetails.getDescription());
		userGroup.setMembers(groupEmailDetails.getMembers());
		return userGroup;
	}
	public static Contacts convertContactFlagsToContacts(ContactsFlagInfo contactsFlagInfo) {
		Contacts contacts = new Contacts();
		//contacts.setEmailAddresses(contactsFlagInfo.getEmailAddress());
		contacts.setNotes(contactsFlagInfo.getNotes());
		contacts.setFirstName(contactsFlagInfo.getName());
		return contacts;
	}
	
	
	
}
