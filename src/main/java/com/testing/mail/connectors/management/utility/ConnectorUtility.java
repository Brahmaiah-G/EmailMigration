package com.testing.mail.connectors.management.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.utils.EmailUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectorUtility {

	
	private ConnectorUtility() {
	}
	
	public static  List<String> mapMails(List<String>to,Map<String,String>mappedPairs) {
		List<String> attends = new ArrayList<>();
		if(to==null || to.isEmpty()) {
			return attends;
		}
		for(String attendees : to) {
			String _attendees = attendees.split(":")[0];
			if(mappedPairs.containsKey(_attendees)) {
				attends.add(mappedPairs.get(_attendees));
			}else {
				attends.add(_attendees);
			}
		}
		return attends;
	}
	
	public static  List<String> mapMailsForGroups(List<String>to,Set<String>groupDetails) {
		List<String> attends = new ArrayList<>();
		if(to==null || to.isEmpty()) {
			return attends;
		}
		for(String attendees : to) {
			String _attendees = attendees.split(":")[0];
			if(groupDetails.contains(_attendees)) {
				continue;
			}
			if(attendees.split(":").length>1) {
				boolean isGRoup = Boolean.parseBoolean(attendees.split(":")[1]);
				if(isGRoup ) {
					groupDetails.add(_attendees);
				}
			}
		}
		return attends;
	}
	
	public static boolean checkMailFoldersCompatability(String mailFolder) {
		return (EmailUtils.EmailKeys.UNREAD.name().equals(mailFolder) || EmailUtils.EmailKeys.STARRED.name().equals(mailFolder));
	}
	
	
	public static String splitNameFromHeader(String value) {
		if(StringUtils.isNotBlank(value) && value.contains("<")) {
			return value.split("<")[0].trim().replace("\"", "").trim();
		}
		return value;
	}

	public static String splitEmailFromHeader(String value) {
		if(StringUtils.isNotBlank(value) && value.contains("<")) {
			return value.split("<")[1].replace(">", "").trim().replace("\"", "").trim();
		}
		return value;
	}

	public static String decodeToString(String byte64) {
		if(StringUtils.isEmpty(byte64)) {
			return null;
		}
		try {
			return new String(Base64.decodeBase64(byte64));
		} catch (Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return null;
		}
	}
	
	public static boolean isEmptyOrNullString(String value) {
		return (StringUtils.isBlank(value)||value.isEmpty() ||value.equals("null"));
	}
	
}
