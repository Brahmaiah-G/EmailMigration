package com.testing.mail.connectors;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.testing.mail.connectors.microsoft.data.AttachmentsData;
import com.testing.mail.contacts.dao.ContactsFlagInfo;
import com.testing.mail.contacts.entities.Contacts;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.dao.entities.ConnectFlags;
import com.testing.mail.dao.entities.EMailRules;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.dao.entities.EmailUserSettings;
import com.testing.mail.dao.entities.UserGroups;
import com.testing.mail.exceptions.MailCreationException;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.repo.entities.CalenderInfo;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;

/**
 * Parent Interface for the Connectors
 * <pre>
 *  Ex : GMailConnector , OutLookMailConnector
 * </pre>
 * @see com.testing.mail.connectors.impl.GMailConnector &#64;GMailConnector
 * @see com.testing.mail.connectors.impl.OutLookMailConnector &#64;OutLookMailConnector
*/

public interface MailConnectors {

	public List<EmailFlagsInfo> getListOfMails(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException;
	public List<EmailFlagsInfo> getListOfMailFolders(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException;
	public EmailFlagsInfo getMailById(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException;
	public EmailInfo createAMailFolder(EmailFlagsInfo emailFlagsInfo) throws MailCreationException;
	public EmailInfo sendEmail(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException;
	public Clouds getAdminDetails(ConnectFlags connectFlags);
	public List<Clouds> getUsersList(ConnectFlags connectFlags);
	public String getDeltaChangeId(EmailFlagsInfo connectFlags);
	public List<AttachmentsData> getAttachments(EmailFlagsInfo emailFlagsInfo) throws MailMigrationException;
	public EmailInfo getLabel(EmailFlagsInfo emailFlagsInfo);
	List<Clouds> getDeltaUsersList(ConnectFlags connectFlags);
	public List<EmailFlagsInfo> getDeltaChanges(EmailFlagsInfo emailFlagsInfo, String deltaChangeId)
			throws MailMigrationException;
	EmailInfo moveEmails(EmailFlagsInfo emailFlagsInfo, EmailInfo emailInfo);
	List<String> getDomains(ConnectFlags connectFlags);
	EmailInfo updateMetadata(EmailFlagsInfo emailFlagsInfo) throws Exception;
	List<CalenderInfo> getCalendarEvents(CalenderFlags emailFlagsInfo);
	CalenderInfo createCalender(CalenderFlags calenderFlags);
	CalenderInfo createCalenderEvent(CalenderFlags calenderFlags);
	List<CalenderInfo> getCalendars(CalenderFlags emailFlagsInfo);
	CalenderInfo getCalendar(CalenderFlags emailFlagsInfo);
	CalenderInfo updateCalendarMetadata(CalenderFlags emailFlagsInfo) throws Exception;
	List<String> addAttachment(EmailFlagsInfo emailFlagsInfo, boolean event) throws IOException;
	boolean deleteEmails(EmailFlagsInfo emailFlagsInfo, boolean event);
	List<EMailRules> getMailBoxRules(EmailFlagsInfo emailFlagsInfo);
	EMailRules createMailBoxRule(EMailRules eMailRules, EmailFlagsInfo emailFlagsInfo);
	List<EmailUserSettings> getSettings(EmailFlagsInfo emailFlagsInfo);
	EmailUserSettings createUpdateSettings(EmailUserSettings emailUserSettings, EmailFlagsInfo emailFlagsInfo);
	List<UserGroups> getGroupEmailDetails(String adminCloudId);
	List<String> getMembersFromGroup(String adminCloudId, String groupId);
	UserGroups getSingleGroupEmailDetails(String adminCloudId, String email);
	List<String> addMembersToGroup(String adminCloudId, List<String> members, String groupId);
	UserGroups createGroup(String adminCloudId, String email, String description, String name, List<String> members);
	List<Contacts> listContacts(ContactsFlagInfo contactsFlagInfo);
	List<CalenderInfo> getEventInstances(CalenderFlags emailFlagsInfo);
}
