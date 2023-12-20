package com.cloudfuze.mail.utils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.Clouds.CLOUD_NAME;
import com.cloudfuze.mail.repo.impl.CloudsRepoImpl;

public class MappingUtils {


	public enum MAIL_FOLDERS{
		SPAM,INBOX,SENT,DRAFT,TRASH,SENTITEMS,JUNKEMAIL,DRAFTS,OUTBOX,DELETEDITEMS,ARCHIVE,CATEGORY_PERSONAL,CATEGORY_PROMOTIONAL,IMPORTANT,STARRED,UNREAD,CATEGORY_UPDATES,CATEGORY_PROMOTIONS,CATEGORY_FORUMS;
	}

	public static String mapMailFolder(String folder,CLOUD_NAME fromCloud,CLOUD_NAME toCloud) {

		String mappedFolder = isCustomFolder(folder)?folder:folder.replace(" ", "");
		if(fromCloud.equals(CLOUD_NAME.OUTLOOK) && toCloud.equals(CLOUD_NAME.GMAIL)) {
			if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.JUNKEMAIL.name())) {
				mappedFolder = MAIL_FOLDERS.SPAM.name();
			}else if(mappedFolder!=null && (mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.SENTITEMS.name())|| mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.OUTBOX.name()))) {
				mappedFolder = MAIL_FOLDERS.SENT.name();
			}else if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.DRAFTS.name())) {
				mappedFolder = MAIL_FOLDERS.DRAFT.name();
			}else if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.DELETEDITEMS.name())) {
				mappedFolder = MAIL_FOLDERS.TRASH.name();
			}
			if(!isCustomFolder(mappedFolder)) {
				mappedFolder = mappedFolder.toUpperCase();
			}
		}else if(fromCloud.equals(CLOUD_NAME.GMAIL) && toCloud.equals(CLOUD_NAME.OUTLOOK)){
			if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.SPAM.name())) {
				mappedFolder = MAIL_FOLDERS.JUNKEMAIL.name().toLowerCase();
			}else if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.SENT.name())) {
				mappedFolder = MAIL_FOLDERS.SENTITEMS.name().toLowerCase();
			}else if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.DRAFT.name())) {
				mappedFolder =MAIL_FOLDERS.DRAFTS.name().toLowerCase();
			}else if(mappedFolder!=null && mappedFolder.equalsIgnoreCase(MAIL_FOLDERS.TRASH.name())) {
				mappedFolder = MAIL_FOLDERS.DELETEDITEMS.name().toLowerCase();
			}
		}else if(fromCloud.equals(CLOUD_NAME.GMAIL) && toCloud.equals(CLOUD_NAME.GMAIL) && !isCustomFolder(mappedFolder)){
			mappedFolder = mappedFolder.toUpperCase();
		}
		return mappedFolder;
	}

	/**
	 * it will check whether the mailFolder is custom or not
	 * @return True if customFolder
	 * {@code
	 * Ex: label_1,subFolder
	 * }
	 *  */
	public static boolean isCustomFolder(String mailFolder) {
		if(mailFolder==null || mailFolder.isEmpty()) {
			return false;
		}
		List<String> systemFolders = Stream.of(MAIL_FOLDERS.values())
				.map(Enum::name)
				.collect(Collectors.toList());
		return !systemFolders.contains(mailFolder.replace(" ", "").trim().toUpperCase());
	}

	public static String checkGoogleMailFolder(List<String> folders) {
		String value = null;
		List<String> specificElements = Arrays.asList(MAIL_FOLDERS.INBOX.name(),MAIL_FOLDERS.SPAM.name(),MAIL_FOLDERS.TRASH.name(),MAIL_FOLDERS.SENT.name(),MAIL_FOLDERS.DRAFT.name());
		for(String folder : folders) {
			if(specificElements.contains(folder)) {
				value = folder;
			}else if(MappingUtils.isCustomFolder(folder)){
				value = folder;
				break;
			}
		}
		return value;
	}


	public static boolean checkSource(String folder,CLOUD_NAME fromCloud) {
		if(folder.startsWith("/")) {
			folder = folder.substring(1,folder.length());
		}
		if(fromCloud.equals(CLOUD_NAME.OUTLOOK)) {
			if(folder!=null &&(folder.equalsIgnoreCase("inbox")|| folder.equalsIgnoreCase("junkemail") || folder.equalsIgnoreCase("sentitems") || folder.equalsIgnoreCase("drafts") || folder.equalsIgnoreCase("outbox") || folder.equalsIgnoreCase("deleteditems"))) {
				return true;
			}
		}else {
			if(folder!=null && (folder.equalsIgnoreCase("spam") || folder.equalsIgnoreCase("sent") || folder.equalsIgnoreCase("draft") || folder.equalsIgnoreCase("trash") || folder.equalsIgnoreCase("inbox"))) {
				return true;
			}
		}
		return false;
	}



	public static boolean isGoogleCombo(CLOUD_NAME fromCloud) {
		return fromCloud.equals(CLOUD_NAME.GMAIL);
	}

	public static boolean isGoogleCombo(CLOUD_NAME fromCloud,CLOUD_NAME toCloud) {
		return fromCloud.equals(CLOUD_NAME.GMAIL) && toCloud.equals(CLOUD_NAME.GMAIL);
	}

	public static boolean isOutlookCombo(CLOUD_NAME fromCloud) {
		return fromCloud.equals(CLOUD_NAME.OUTLOOK);
	}

	public static <T, E> T getKeysByValue(Map<T, E> map, E value) {
		return map.entrySet()
				.stream()
				.filter(entry -> Objects.equals(entry.getValue(), value))
				.findFirst().get().getKey();
	}

	public static long setPriority(String mailBox) {
		long id=5;
		if(mailBox.equalsIgnoreCase(MAIL_FOLDERS.SENT.name()) || mailBox.equalsIgnoreCase(MAIL_FOLDERS.SENTITEMS.name())) {
			id=0;
		}else if(mailBox.equalsIgnoreCase(MAIL_FOLDERS.DRAFT.name()) || mailBox.equalsIgnoreCase(MAIL_FOLDERS.DRAFTS.name())) {
			id=1;
		}else if(mailBox.equalsIgnoreCase(MAIL_FOLDERS.INBOX.name())) {
			id=2;
		}else if(mailBox.equalsIgnoreCase(MAIL_FOLDERS.SPAM.name()) || mailBox.equalsIgnoreCase(MAIL_FOLDERS.JUNKEMAIL.name())) {
			id=3;
		}else if(mailBox.equalsIgnoreCase(MAIL_FOLDERS.TRASH.name()) || mailBox.equalsIgnoreCase(MAIL_FOLDERS.DELETEDITEMS.name())) {
			id=4;
		}
		return id;
	}

	public static boolean checkOrganizerWithSourceEmail(String emailId,String userId,String adminMemberId,String cloudEmail,CloudsRepoImpl cloudsRepoImpl) {
		if(cloudEmail.equals(emailId)) {
			return false;
		}
		Clouds cloud = cloudsRepoImpl.findCloudsByEmailIdUsingAdmin(userId, emailId, adminMemberId);
		if(cloud!=null) {
			return true;
		}
		return false;
	}

	public static boolean checkOrganizerExists(String emailId,String userId,String adminMemberId,CloudsRepoImpl cloudsRepoImpl) {
		Clouds cloud = cloudsRepoImpl.findCloudsByEmailIdUsingAdmin(userId, emailId, adminMemberId);
		if(cloud!=null) {
			return true;
		}
		return false;
	}

	public  static String formatFileSize(long size) {
		String fileSize = null;

		double b = size;
		double k = size/1024.0;
		double m = ((size/1024.0)/1024.0);
		double g = (((size/1024.0)/1024.0)/1024.0);
		double t = ((((size/1024.0)/1024.0)/1024.0)/1024.0);
		DecimalFormat dec = new DecimalFormat("0.00");
		if ( t>1 ) {
			fileSize = dec.format(t).concat(" TB");
		} else if ( g>1 ) {
			fileSize = dec.format(g).concat(" GB");
		} else if ( m>1 ) {
			fileSize = dec.format(m).concat(" MB");
		} else if ( k>1 ) {
			fileSize = dec.format(k).concat(" KB");
		} else {
			fileSize = dec.format(b).concat(" Bytes");
		}
		return fileSize;
	}


}
