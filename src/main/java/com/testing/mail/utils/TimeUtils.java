package com.testing.mail.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeUtils {

	private TimeUtils(){
	}
	
	static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static LocalDateTime convertStringToTime(String time) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT);
			return LocalDateTime.parse(time, formatter);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return LocalDateTime.now();
		}
	}
	public static LocalDateTime convertStringToTimeFor(String time,String inputTimeZone) {
		try {
			if(time.length()>26) {
				time= convertOutlookTimeFormatWithOffset(time, inputTimeZone);
			}
			//2023-09-28T10:30:00+05:30
			if(time.split("\\-").length>3) {
				time = time.substring(0, time.lastIndexOf("-"));
			}else {
				time = time.split("\\+")[0];
			}
			//if(time.contains(arg0))
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
			return LocalDateTime.parse(time, formatter);
		}catch(Exception e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return LocalDateTime.now();
		}
	}

	public static String convertLongToTime(String time,String timeZone) {
		String formatted = null;
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT);
			return Instant.ofEpochMilli(Long.valueOf(time)).atZone(ZoneId.of("UTC")).format(formatter);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			formatted = new Date().toLocaleString();
		}
		return formatted;
	}


	public static String convertGoogleTimeFormat(String time,String timeZone) {
		try {
			if(time!=null && !time.endsWith("Z")) {
				time = time+"Z";
			}
			Instant instant = Instant.parse(time);
			ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss Z");
			String modifiedTime =  zonedDateTime.format(formatter);
			//modifiedTime =modifiedTime + ZoneId.of(timeZone).getRules().getOffset(instant).toString().replace(":", "");
			return modifiedTime;
		} catch (Exception e) {
			return new Date().toLocaleString();
		}
	}
	
	
	public static String convertGoogleTimeFormatWithOffset(String inputDateTime,String timeZone) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

		LocalDateTime localDateTime = LocalDateTime.parse(inputDateTime, formatter);
		ZoneId zoneId = ZoneId.of(timeZone);
		ZoneOffset zoneOffset = zoneId.getRules().getOffset(java.time.Instant.now());
		OffsetDateTime offsetDateTime = localDateTime.atOffset(zoneOffset);
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
		return offsetDateTime.format(outputFormatter);
	}
	
	public static String convertOutlookTimeFormatWithOffset(String inputTimestamp,String inputTimeZone) {
		//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.ssXXXzz");
		inputTimestamp = inputTimestamp.substring(0, 26) + "Z";
		// Parse the input timestamp and set the time zone
		Instant instant = Instant.parse(inputTimestamp);
		ZoneId zoneId = ZoneId.of(inputTimeZone);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);

		// Format the output timestamp
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		return zonedDateTime.format(formatter);
	}
	
	public static String convertGoogleTimeFormatForEvents(String inputDateTime) {
		if(null!=inputDateTime && inputDateTime.contains(".")) {
			return inputDateTime.split("\\.")[0];
		}
		return inputDateTime;
	}

	public static String convertTimeFormat(String time) {
		String format = "yyyy-MM-dd";
		String formatted = null;
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
			return Instant.ofEpochMilli(Long.valueOf(time)).atZone(ZoneId.systemDefault()).format(formatter);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			formatted = new Date().toLocaleString();
		}
		return formatted;
	}
	/**
	 * Source Time zone to DEstination Time Zone currently support UTC from GOOGLE as Source
	 * <p></p>
	 * Converting the Format of 2023-04-21T02:30:00(UTC) to the diff TimeZone
	 *<p></p>
	 *Used when only the TimeZones are diff then the UTC
	 *<p></p>
	 *@return modifiedTime based on diffTimeZone if having +5.30 then remove that also , if Exception throws return the sourceTime
	*/
	public static String covertTimeZones(String sourceTimeZone,String destTimeZone,String sourceTime) {
		boolean utc = false;
		try {
			sourceTimeZone = sourceTimeZone==null?"UTC":sourceTimeZone;
			sourceTime = sourceTime!=null && (sourceTime.endsWith("+05:30")?utc=true:false)?sourceTime.split("\\+05:30")[0]:sourceTime;
			sourceTime = sourceTime!=null &&sourceTime.lastIndexOf(".")>-1 ?sourceTime.substring(0, sourceTime.lastIndexOf(".")) : sourceTime;
			//if both time zones are same not required to convert anything
			if(sourceTimeZone.equalsIgnoreCase(destTimeZone)) {
				return sourceTime;
			}
			String inputDateTimeStr = sourceTime;
			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
			LocalDateTime localDateTime = LocalDateTime.parse(inputDateTimeStr, inputFormatter);
			ZoneId destZoneId = ZoneId.of(sourceTimeZone);
			ZonedDateTime destDateTime = ZonedDateTime.of(localDateTime, destZoneId);

			ZoneId utcZoneId = ZoneId.of(destTimeZone);
			ZonedDateTime convertedDateTime = destDateTime.withZoneSameInstant(utcZoneId);
			if(utc) {
				//removing if +5.30 had been added so that we will get original time as outlook converting it self in the destination
				convertedDateTime = convertedDateTime.minusHours(5).minusMinutes(30);
			}

			DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
			return outputFormatter.format(convertedDateTime);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return sourceTime;
	}
	
	public static Map<String,String> loadTimeZones(){
		Map<String,String>TIMEZONE_MAPPINGS = new HashMap<>(175);
		TIMEZONE_MAPPINGS.put("Romance", "Europe/Paris");
        TIMEZONE_MAPPINGS.put("Romance Standard Time", "Europe/Paris");
        TIMEZONE_MAPPINGS.put("Warsaw", "Europe/Warsaw");
        TIMEZONE_MAPPINGS.put("Central Europe", "Europe/Prague");
        TIMEZONE_MAPPINGS.put("Central Europe Standard Time", "Europe/Prague");
        TIMEZONE_MAPPINGS.put("Prague Bratislava", "Europe/Prague");
        TIMEZONE_MAPPINGS.put("W. Central Africa Standard Time","Africa/Luanda");
        TIMEZONE_MAPPINGS.put("FLE", "Europe/Helsinki");
        TIMEZONE_MAPPINGS.put("FLE Standard Time", "Europe/Helsinki");
        TIMEZONE_MAPPINGS.put("GFT", "Europe/Athens");
        TIMEZONE_MAPPINGS.put("GFT Standard Time", "Europe/Athens");
        TIMEZONE_MAPPINGS.put("GTB", "Europe/Athens");
        TIMEZONE_MAPPINGS.put("GTB Standard Time", "Europe/Athens");
        TIMEZONE_MAPPINGS.put("Israel", "Asia/Jerusalem");
        TIMEZONE_MAPPINGS.put("Israel Standard Time", "Asia/Jerusalem");
        TIMEZONE_MAPPINGS.put("Arab", "Asia/Riyadh");
        TIMEZONE_MAPPINGS.put("Arab Standard Time", "Asia/Riyadh");
        TIMEZONE_MAPPINGS.put("Arabic Standard Time", "Asia/Baghdad");
        TIMEZONE_MAPPINGS.put("E. Africa", "Africa/Nairobi");
        TIMEZONE_MAPPINGS.put("E. Africa Standard Time", "Africa/Nairobi");
        TIMEZONE_MAPPINGS.put("Saudi Arabia", "Asia/Riyadh");
        TIMEZONE_MAPPINGS.put("Saudi Arabia Standard Time", "Asia/Riyadh");
        TIMEZONE_MAPPINGS.put("Iran", "Asia/Tehran");
        TIMEZONE_MAPPINGS.put("Iran Standard Time", "Asia/Tehran");
        TIMEZONE_MAPPINGS.put("Afghanistan", "Asia/Kabul");
        TIMEZONE_MAPPINGS.put("Afghanistan Standard Time", "Asia/Kabul");
        TIMEZONE_MAPPINGS.put("India", "Asia/Calcutta");
        TIMEZONE_MAPPINGS.put("India Standard Time", "Asia/Calcutta");
        TIMEZONE_MAPPINGS.put("Myanmar Standard Time", "Asia/Rangoon");
        TIMEZONE_MAPPINGS.put("Nepal Standard Time", "Asia/Katmandu");
        TIMEZONE_MAPPINGS.put("Sri Lanka", "Asia/Colombo");
        TIMEZONE_MAPPINGS.put("Sri Lanka Standard Time", "Asia/Colombo");
        TIMEZONE_MAPPINGS.put("Beijing", "Asia/Shanghai");
        TIMEZONE_MAPPINGS.put("China", "Asia/Shanghai");
        TIMEZONE_MAPPINGS.put("China Standard Time", "Asia/Shanghai");
        TIMEZONE_MAPPINGS.put("AUS Central", "Australia/Darwin");
        TIMEZONE_MAPPINGS.put("AUS Central Standard Time", "Australia/Darwin");
        TIMEZONE_MAPPINGS.put("Cen. Australia", "Australia/Adelaide");
        TIMEZONE_MAPPINGS.put("Cen. Australia Standard Time","Australia/Adelaide");
        TIMEZONE_MAPPINGS.put("Vladivostok", "Asia/Vladivostok");
        TIMEZONE_MAPPINGS.put("Vladivostok Standard Time", "Asia/Vladivostok");
        TIMEZONE_MAPPINGS.put("West Pacific", "Pacific/Guam");
        TIMEZONE_MAPPINGS.put("West Pacific Standard Time", "Pacific/Guam");
        TIMEZONE_MAPPINGS.put("E. South America", "America/Sao_Paulo");
        TIMEZONE_MAPPINGS.put("E. South America Standard Time","America/Sao_Paulo");
        TIMEZONE_MAPPINGS.put("Greenland Standard Time", "America/Godthab");
        TIMEZONE_MAPPINGS.put("Newfoundland", "America/St_Johns");
        TIMEZONE_MAPPINGS.put("Newfoundland Standard Time", "America/St_Johns");
        TIMEZONE_MAPPINGS.put("Pacific SA", "America/Caracas");
        TIMEZONE_MAPPINGS.put("Pacific SA Standard Time", "America/Caracas");
        TIMEZONE_MAPPINGS.put("SA Western", "America/Caracas");
        TIMEZONE_MAPPINGS.put("SA Western Standard Time", "America/Caracas");
        TIMEZONE_MAPPINGS.put("SA Pacific", "America/Bogota");
        TIMEZONE_MAPPINGS.put("SA Pacific Standard Time", "America/Bogota");
        TIMEZONE_MAPPINGS.put("US Eastern", "America/Indianapolis");
        TIMEZONE_MAPPINGS.put("US Eastern Standard Time","America/Indianapolis");
        TIMEZONE_MAPPINGS.put("Central America Standard Time","America/Regina");
        TIMEZONE_MAPPINGS.put("Mexico", "America/Mexico_City");
        TIMEZONE_MAPPINGS.put("Eastern Standard Time (Mexico)", "America/Mexico_City");
        TIMEZONE_MAPPINGS.put("Mexico Standard Time", "America/Mexico_City");
        TIMEZONE_MAPPINGS.put("Canada Central", "America/Regina");
        TIMEZONE_MAPPINGS.put("Canada Central Standard Time", "America/Regina");
        TIMEZONE_MAPPINGS.put("US Mountain", "America/Phoenix");
        TIMEZONE_MAPPINGS.put("US Mountain Standard Time", "America/Phoenix");
        TIMEZONE_MAPPINGS.put("GMT", "Europe/London");
        TIMEZONE_MAPPINGS.put("Ekaterinburg", "Asia/Yekaterinburg");
        TIMEZONE_MAPPINGS.put("Ekaterinburg Standard Time","Asia/Yekaterinburg");
        TIMEZONE_MAPPINGS.put("West Asia", "Asia/Karachi");
        TIMEZONE_MAPPINGS.put("West Asia Standard Time", "Asia/Karachi");
        TIMEZONE_MAPPINGS.put("Central Asia", "Asia/Dhaka");
        TIMEZONE_MAPPINGS.put("Central Asia Standard Time", "Asia/Dhaka");
        TIMEZONE_MAPPINGS.put("N. Central Asia Standard Time","Asia/Novosibirsk");
        TIMEZONE_MAPPINGS.put("Bangkok", "Asia/Bangkok");
        TIMEZONE_MAPPINGS.put("Bangkok Standard Time", "Asia/Bangkok");
        TIMEZONE_MAPPINGS.put("North Asia Standard Time", "Asia/Krasnoyarsk");
        TIMEZONE_MAPPINGS.put("SE Asia", "Asia/Bangkok");
        TIMEZONE_MAPPINGS.put("SE Asia Standard Time", "Asia/Bangkok");
        TIMEZONE_MAPPINGS.put("North Asia East Standard Time","Asia/Ulaanbaatar");
        TIMEZONE_MAPPINGS.put("Singapore", "Asia/Singapore");
        TIMEZONE_MAPPINGS.put("Singapore Standard Time", "Asia/Singapore");
        TIMEZONE_MAPPINGS.put("Taipei", "Asia/Taipei");
        TIMEZONE_MAPPINGS.put("Taipei Standard Time", "Asia/Taipei");
        TIMEZONE_MAPPINGS.put("W. Australia", "Australia/Perth");
        TIMEZONE_MAPPINGS.put("W. Australia Standard Time", "Australia/Perth");
        TIMEZONE_MAPPINGS.put("Korea", "Asia/Seoul");
        TIMEZONE_MAPPINGS.put("Korea Standard Time", "Asia/Seoul");
        TIMEZONE_MAPPINGS.put("Tokyo", "Asia/Tokyo");
        TIMEZONE_MAPPINGS.put("Tokyo Standard Time", "Asia/Tokyo");
        TIMEZONE_MAPPINGS.put("Yakutsk", "Asia/Yakutsk");
        TIMEZONE_MAPPINGS.put("Yakutsk Standard Time", "Asia/Yakutsk");
        TIMEZONE_MAPPINGS.put("Central European", "Europe/Belgrade");
        TIMEZONE_MAPPINGS.put("Central European Standard Time", "Europe/Belgrade");
        TIMEZONE_MAPPINGS.put("W. Europe", "Europe/Berlin");
        TIMEZONE_MAPPINGS.put("W. Europe Standard Time", "Europe/Berlin");
        TIMEZONE_MAPPINGS.put("Tasmania", "Australia/Hobart");
        TIMEZONE_MAPPINGS.put("Tasmania Standard Time", "Australia/Hobart");
        TIMEZONE_MAPPINGS.put("AUS Eastern", "Australia/Sydney");
        TIMEZONE_MAPPINGS.put("AUS Eastern Standard Time", "Australia/Sydney");
        TIMEZONE_MAPPINGS.put("E. Australia", "Australia/Brisbane");
        TIMEZONE_MAPPINGS.put("E. Australia Standard Time", "Australia/Brisbane");
        TIMEZONE_MAPPINGS.put("Sydney Standard Time", "Australia/Sydney");
        TIMEZONE_MAPPINGS.put("Central Pacific", "Pacific/Guadalcanal");
        TIMEZONE_MAPPINGS.put("Central Pacific Standard Time","Pacific/Guadalcanal");
        TIMEZONE_MAPPINGS.put("Dateline", "Pacific/Majuro");
        TIMEZONE_MAPPINGS.put("Dateline Standard Time", "Pacific/Majuro");
        TIMEZONE_MAPPINGS.put("Fiji", "Pacific/Fiji");
        TIMEZONE_MAPPINGS.put("Fiji Standard Time", "Pacific/Fiji");
        TIMEZONE_MAPPINGS.put("Samoa", "Pacific/Apia");
        TIMEZONE_MAPPINGS.put("Samoa Standard Time", "Pacific/Apia");
        TIMEZONE_MAPPINGS.put("Hawaiian", "Pacific/Honolulu");
        TIMEZONE_MAPPINGS.put("Hawaiian Standard Time", "Pacific/Honolulu");
        TIMEZONE_MAPPINGS.put("Alaskan", "America/Anchorage");
        TIMEZONE_MAPPINGS.put("Alaskan Standard Time", "America/Anchorage");
        TIMEZONE_MAPPINGS.put("Pacific", "America/Los_Angeles");
        TIMEZONE_MAPPINGS.put("Pacific Standard Time", "America/Los_Angeles");
        TIMEZONE_MAPPINGS.put("Mexico Standard Time 2", "America/Chihuahua");
        TIMEZONE_MAPPINGS.put("Mountain", "America/Denver");
        TIMEZONE_MAPPINGS.put("Mountain Standard Time", "America/Denver");
        TIMEZONE_MAPPINGS.put("Central", "America/Chicago");
        TIMEZONE_MAPPINGS.put("Central Standard Time", "America/Chicago");
        TIMEZONE_MAPPINGS.put("Eastern", "America/New_York");
        TIMEZONE_MAPPINGS.put("Eastern Standard Time", "America/New_York");
        TIMEZONE_MAPPINGS.put("E. Europe", "Europe/Bucharest");
        TIMEZONE_MAPPINGS.put("E. Europe Standard Time", "Europe/Bucharest");
        TIMEZONE_MAPPINGS.put("Egypt", "Africa/Cairo");
        TIMEZONE_MAPPINGS.put("Egypt Standard Time", "Africa/Cairo");
        TIMEZONE_MAPPINGS.put("South Africa", "Africa/Harare");
        TIMEZONE_MAPPINGS.put("South Africa Standard Time", "Africa/Harare");
        TIMEZONE_MAPPINGS.put("Atlantic", "America/Halifax");
        TIMEZONE_MAPPINGS.put("Atlantic Standard Time", "America/Halifax");
        TIMEZONE_MAPPINGS.put("SA Eastern", "America/Buenos_Aires");
        TIMEZONE_MAPPINGS.put("SA Eastern Standard Time", "America/Buenos_Aires");
        TIMEZONE_MAPPINGS.put("Mid-Atlantic", "Atlantic/South_Georgia");
        TIMEZONE_MAPPINGS.put("Mid-Atlantic Standard Time","Atlantic/South_Georgia");
        TIMEZONE_MAPPINGS.put("Azores", "Atlantic/Azores");
        TIMEZONE_MAPPINGS.put("Azores Standard Time", "Atlantic/Azores");
        TIMEZONE_MAPPINGS.put("Cape Verde Standard Time", "Atlantic/Cape_Verde");
        TIMEZONE_MAPPINGS.put("Russian", "Europe/Moscow");
        TIMEZONE_MAPPINGS.put("Russian Standard Time", "Europe/Moscow");
        TIMEZONE_MAPPINGS.put("New Zealand", "Pacific/Auckland");
        TIMEZONE_MAPPINGS.put("New Zealand Standard Time", "Pacific/Auckland");
        TIMEZONE_MAPPINGS.put("Tonga Standard Time", "Pacific/Tongatapu");
        TIMEZONE_MAPPINGS.put("Venezuela Standard Time", "America/Caracas");
        TIMEZONE_MAPPINGS.put("Arabian", "Asia/Muscat");
        TIMEZONE_MAPPINGS.put("Arabian Standard Time", "Asia/Muscat");
        TIMEZONE_MAPPINGS.put("Caucasus", "Asia/Tbilisi");
        TIMEZONE_MAPPINGS.put("Caucasus Standard Time", "Asia/Tbilisi");
        TIMEZONE_MAPPINGS.put("GMT Standard Time", "GMT");
        TIMEZONE_MAPPINGS.put("Greenwich", "GMT");
        TIMEZONE_MAPPINGS.put("Greenwich Standard Time", "GMT");
		return TIMEZONE_MAPPINGS;
	}
	
	public static LocalDate convertTimeToLocalDate(String time) {
		String format = "yyyy-MM-dd";
		LocalDate formatted = null;
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
			return LocalDate.parse(time, formatter);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			formatted = LocalDate.now();
		}
		return formatted;
	}
	
	public static LocalDate convertRecurenceTimeToLocalDate(String time) {
		String format = "yyyyMMdd";
		LocalDate formatted = null;
		if(time.length()>26) {
			time= convertOutlookTimeFormatWithOffset(time, "UTC");
		}
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
			return LocalDate.parse(time, formatter);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			formatted = LocalDate.now();
		}
		return formatted;
	}
	
	public static LocalDate convertRecurenceTimeToLocalDate(String time,String timeZone) {
		LocalDate formatted = null;
		try {
			String _time = time.split("Z")[0];
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss[XXX][X]");
	        OffsetDateTime offsetDateTime = OffsetDateTime.parse(_time + "Z", formatter);
			ZonedDateTime zonedDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(timeZone));
			String modifiedTime =  zonedDateTime.format(formatter);
			return LocalDate.parse(modifiedTime, formatter);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			formatted = LocalDate.now();
		}
		return formatted;
	}
	
	public static LocalDateTime convertRecurenceTimeToLocalDateTime(String time,String timeZone) {
		LocalDateTime formatted = null;
		try {
			if(time.split("\\-").length>3) {
				time = time.substring(0, time.lastIndexOf("-"));
			}else {
				time = time.split("\\+")[0];
			}
			String _time = time.split("Z")[0];
			_time = _time.replace(":", "");
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss[XXX][X]");
	        OffsetDateTime offsetDateTime = OffsetDateTime.parse(_time + "Z", formatter);
			ZonedDateTime zonedDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(timeZone));
			String modifiedTime =  zonedDateTime.format(formatter);
			return LocalDateTime.parse(modifiedTime, formatter);
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
			formatted = LocalDateTime.now();
		}
		return formatted;
	}
	
	
	public static boolean checkEventType(String endTime,String timeZone) {
		if(!endTime.contains(":")) {
			LocalDate date = convertTimeToLocalDate(endTime);
			if(date!=null && date.isAfter(LocalDate.now())) {
				return true;
			}
		}else {
			LocalDateTime end = convertStringToTimeFor(endTime,timeZone);
			if(end!=null && end.isAfter(LocalDateTime.now())) {
				return true;
			}
		}
		return false;
	}
	
	
}
