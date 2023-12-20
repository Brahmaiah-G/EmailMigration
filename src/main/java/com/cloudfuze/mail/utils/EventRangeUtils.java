package com.cloudfuze.mail.utils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.cloudfuze.mail.constants.Const;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EventRangeUtils {
	private String type;
	private String days;
	private String wkst;
	private String endDate;
	private String timeZone;
	private String startDate;
	private String index;
	private String occurences;
	private String interval;
	private LocalDate lastOccurence;
	private String recurenceType;
	
	
	public static String getModifiedType(String type) {
		if(StringUtils.isNotBlank(type) && type.equalsIgnoreCase("YEARLY")) {
			type = "relativeYearly";
		}else if(StringUtils.isNotBlank(type) && type.equalsIgnoreCase("MONTHLY")) {
			type = "relativeMonthly";
		}
		return type;
	}
	
	public static EventRangeUtils setRangeForGmailAsSource(String rule,String recurenceType) {
		//For the recurrence range getting 
		//Range in the form of  Type#startDate#endDate#FirstDay#WeekDays#interval from Outlook
		EventRangeUtils eventRangeUtils = new EventRangeUtils();
		if(rule!=null) {
			List<String> ranges = Arrays.asList(rule.split(Const.HASHTAG));
			eventRangeUtils.setType(ranges.get(0));
			eventRangeUtils.setStartDate(ranges.get(1));
			if(eventRangeUtils.getType().equalsIgnoreCase("noEnd") && (ranges.get(4)!=null && !ranges.get(4).equals("null"))) {
				if(ranges.get(4).matches(".*\\d+.*")) {
					String number = ranges.get(4).replaceAll("[^0-9]", "");
					if(StringUtils.isNotBlank(number)) {
						eventRangeUtils.setIndex(number);
						ranges.set(4, ranges.get(4).replace(number, ""));
					}
				}
			}else if(ranges.get(4)!=null && !ranges.get(4).equals("null")) {
				if(ranges.get(4).matches(".*\\d+.*")) {
					String number = ranges.get(4).replaceAll("[^0-9]", "");
					if(StringUtils.isNotBlank(number)) {
						eventRangeUtils.setIndex(number);
						ranges.set(4, ranges.get(4).replace(number, ""));
					}
				}
			}
			eventRangeUtils.setEndDate(ranges.get(2).equals("null")?null:ranges.get(2));
			eventRangeUtils.setWkst(ranges.get(3));
			eventRangeUtils.setDays(ranges.get(4));
			eventRangeUtils.setOccurences(ranges.get(5).equals("null")?null:ranges.get(5));
			eventRangeUtils.setInterval(ranges.get(6));
			eventRangeUtils.setRecurenceType(recurenceType);
			if(StringUtils.isNotBlank(eventRangeUtils.getOccurences()) && !eventRangeUtils.getOccurences().equals("null")) {
				eventRangeUtils.setLastOccurence(validateLastOccurenceDate(eventRangeUtils));
			}
		}
		return eventRangeUtils;
	}
	
	public static EventRangeUtils setRangeForOutlookAsSource(String rule,String recurenceType) {
		//For the recurrence range getting 
		//Range in the form of  Type#startDate#endDate#FirstDay#WeekDays#interval from Outlook
		EventRangeUtils eventRangeUtils = new EventRangeUtils();
		if(rule!=null) {
			List<String> ranges = Arrays.asList(rule.split(Const.HASHTAG));
			eventRangeUtils.setType(ranges.get(0));
			eventRangeUtils.setStartDate(ranges.get(1));
			if(eventRangeUtils.getType().equalsIgnoreCase("noEnd") && (ranges.get(4)!=null && !ranges.get(4).equals("null"))) {
				if(ranges.get(4).matches(".*\\d+.*")) {
					String number = ranges.get(4).replaceAll("[^0-9]", "");
					if(StringUtils.isNotBlank(number)) {
						eventRangeUtils.setIndex(number);
						ranges.set(4, ranges.get(4).replace(number, ""));
					}
				}
			}else if(ranges.get(4)!=null && !ranges.get(4).equals("null")) {
				if(ranges.get(4).matches(".*\\d+.*")) {
					String number = ranges.get(4).replaceAll("[^0-9]", "");
					if(StringUtils.isNotBlank(number)) {
						eventRangeUtils.setIndex(number);
						ranges.set(4, ranges.get(4).replace(number, ""));
					}
				}
			}
			eventRangeUtils.setEndDate(ranges.get(2).equals("null")?null:ranges.get(2));
			eventRangeUtils.setWkst(ranges.get(3));
			eventRangeUtils.setDays(ranges.get(4));
			eventRangeUtils.setOccurences(ranges.get(5).equals("null")?null:ranges.get(5));
			// need to validate the occurences to the number format
			eventRangeUtils.setInterval(ranges.get(6));
			eventRangeUtils.setRecurenceType(recurenceType);
			if(StringUtils.isNotBlank(eventRangeUtils.getOccurences()) && !eventRangeUtils.getOccurences().equals("null")) {
				eventRangeUtils.setLastOccurence(validateLastOccurenceDate(eventRangeUtils));
			}
		}
		return eventRangeUtils;
	}
	
	public static LocalDate validateLastOccurenceDate(EventRangeUtils eventRangeUtils) {
		try {
			LocalDate date = LocalDate.parse(eventRangeUtils.getStartDate().split("T")[0]);
			if("weekly".equalsIgnoreCase(eventRangeUtils.getRecurenceType())) {
				if(eventRangeUtils.getInterval()!=null && !"null".equals(eventRangeUtils.getInterval())) {
					date = date.plusWeeks(Integer.valueOf(eventRangeUtils.getOccurences())*Integer.valueOf(eventRangeUtils.getInterval()));
					date = date.minusWeeks(Integer.valueOf(eventRangeUtils.getInterval()));
				}else {
					date = date.plusWeeks(Integer.valueOf(eventRangeUtils.getOccurences())-1);
				}
			}else if("YEARLY".equalsIgnoreCase(eventRangeUtils.getRecurenceType())) {
				if(eventRangeUtils.getInterval()!=null && !"null".equals(eventRangeUtils.getInterval())) {
					date = date.plusYears(Integer.valueOf(eventRangeUtils.getOccurences())*Integer.valueOf(eventRangeUtils.getInterval()));
					date = date.minusYears(Integer.valueOf(eventRangeUtils.getInterval()));
				}else {
					date = date.plusYears(Integer.valueOf(eventRangeUtils.getOccurences())-1);
				}
			}else if("relativeYearly".equalsIgnoreCase(eventRangeUtils.getRecurenceType())) {
				if(eventRangeUtils.getInterval()!=null && !"null".equals(eventRangeUtils.getInterval())) {
					date = date.plusYears(Integer.valueOf(eventRangeUtils.getOccurences())*Integer.valueOf(eventRangeUtils.getInterval()));
					date = date.minusYears(Integer.valueOf(eventRangeUtils.getInterval()));
				}else {
					date = date.plusYears(Integer.valueOf(eventRangeUtils.getOccurences())-1);
				}
			}else if("MONTHLY".equalsIgnoreCase(eventRangeUtils.getRecurenceType()) ) {
				if(eventRangeUtils.getInterval()!=null && !"null".equals(eventRangeUtils.getInterval())) {
					date = date.plusMonths(Integer.valueOf(eventRangeUtils.getOccurences())*Integer.valueOf(eventRangeUtils.getInterval()));
					date = date.minusMonths(Integer.valueOf(eventRangeUtils.getInterval()));
				}else {
					date = date.plusMonths(Integer.valueOf(eventRangeUtils.getOccurences())-1);
				}
			}else if("relativeMonthly".equalsIgnoreCase(eventRangeUtils.getRecurenceType())){
				if(eventRangeUtils.getInterval()!=null && !"null".equals(eventRangeUtils.getInterval())) {
					date = date.plusMonths(Integer.valueOf(eventRangeUtils.getOccurences())*Integer.valueOf(eventRangeUtils.getInterval()));
					date = date.minusMonths(Integer.valueOf(eventRangeUtils.getInterval()));
				}else {
					date = date.plusMonths(Integer.valueOf(eventRangeUtils.getOccurences())-1);
				}
			}else {
				if(eventRangeUtils.getInterval()!=null && !"null".equals(eventRangeUtils.getInterval())) {
					date = date.plusDays(Integer.valueOf(eventRangeUtils.getOccurences())*Integer.valueOf(eventRangeUtils.getInterval()));
					date = date.minusDays(Integer.valueOf(eventRangeUtils.getInterval()));
				}else {
					date = date.plusDays(Integer.valueOf(eventRangeUtils.getOccurences()));
				}
			}
			return date;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
