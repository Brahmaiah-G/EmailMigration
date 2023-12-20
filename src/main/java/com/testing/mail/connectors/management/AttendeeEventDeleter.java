package com.testing.mail.connectors.management;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.connectors.microsoft.data.Value;
import com.testing.mail.dao.entities.CalenderFlags;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailWorkSpace;
import com.testing.mail.repo.entities.EventsInfo;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class AttendeeEventDeleter implements Runnable {
	
	
	
	MailServiceFactory mailServiceFactory;
	DBConnectorService connectorService;
	List<String> attendees;
	EventsInfo calenderInfo;
	EmailWorkSpace emailWorkSpace;
	
	

	public AttendeeEventDeleter(MailServiceFactory mailServiceFactory, DBConnectorService dbConnectorService,
			List<String> attendees, EventsInfo info,EmailWorkSpace emailWorkSpace) {
		this.mailServiceFactory = mailServiceFactory;
		this.connectorService = dbConnectorService;
		this.attendees = attendees;
		this.calenderInfo = info;
		this.emailWorkSpace = emailWorkSpace;
	}

	@Override
	public void run() {
		deleteAttendeeEventMessge();
	}
	
	private void deleteAttendeeEventMessge() {
		try {
			log.info("=Entered for deleting the eventMessages in the mailBox of attendees--"+calenderInfo.getId()+"-:"+emailWorkSpace.getId());
			OutLookMailConnector outLookMailConnector =  ((OutLookMailConnector)mailServiceFactory.getConnectorService(emailWorkSpace.getToCloud()));
			List<String>dups = new ArrayList<>();
			CalenderFlags emailFlagsInfo = new CalenderFlags();
			List<String>emds = new ArrayList<>();
			List<String>atendees = new ArrayList<>();
			atendees.addAll(calenderInfo.getAttendees());
			atendees.add(calenderInfo.getOrganizer());
			for(String attendees :calenderInfo.getAttendees()) {
				String _attendees = attendees.split(":")[0];
				if(dups.contains(_attendees) || (emailFlagsInfo.isExternalOrg() &&_attendees.equals(calenderInfo.getOrganizer()))) {
					continue;
				}
				dups.add(_attendees);
				Clouds cloud = connectorService.getCloudsRepoImpl().findCloudsByEmailIdUsingAdmin(calenderInfo.getUserId(), _attendees, emailWorkSpace.getToAdminCloud());
				if(cloud==null) {
					log.info("--For External User Skipping the event Deletion---"+calenderInfo.getId()+"--"+calenderInfo.getSourceId());
					calenderInfo.setErrorDescription(calenderInfo.getErrorDescription()+"-"+_attendees);
					continue;	
				}
				emailFlagsInfo.setCloudId(cloud.getId());
				emailFlagsInfo.setDestId(calenderInfo.getDestId());
				emailFlagsInfo.setICalUId(calenderInfo.getICalUId());
				try {
					boolean toMail = false;
					if(emailWorkSpace.getToMailId().equals(_attendees)) {
						emailFlagsInfo.setCalendar("sentitems");
						toMail = true;
					}else {
						emailFlagsInfo.setCalendar("inbox");
					}
					for(int i =0;i<2;i++) {
						Value created = outLookMailConnector.getEventMessage(emailFlagsInfo);
						if(created!=null) {
							emailFlagsInfo.setId(created.getId());
							boolean deleted = outLookMailConnector.deleteEventMails(emailFlagsInfo);
							if(toMail) {
								emailFlagsInfo.setCalendar("inbox");
							}else {
								break;
							}
							if(deleted) {
								emds.add(_attendees);
							}
						}else {
							emailFlagsInfo.setCalendar("inbox");
						}
					}
				} catch (Exception e) {
					calenderInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
				}
			}
			calenderInfo.setEmdAtendee(emds);
		} catch (Exception e) {
			calenderInfo.setErrorDescription(ExceptionUtils.getStackTrace(e));
		}finally {
			connectorService.getCalendarInfoRepoImpl().save(calenderInfo);
		}
	}
}
