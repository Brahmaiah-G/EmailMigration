package com.testing.mail.connectors.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.testing.mail.connectors.MailConnectors;
import com.testing.mail.connectors.impl.GMailConnector;
import com.testing.mail.connectors.impl.OutLookMailConnector;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MailServiceFactory {

	@Autowired
	OutLookMailConnector outLook;
	
	@Autowired
	GMailConnector gMailConnector;
	
	
	public MailConnectors getConnectorService(CLOUD_NAME vendorName) {
		if(vendorName ==  CLOUD_NAME.OUTLOOK) {
			log.info("-Returing vendor--"+vendorName.name());
			return outLook;
		}else if(vendorName == CLOUD_NAME.GMAIL) {
			log.info("-Returing vendor--"+vendorName.name());
			return gMailConnector;
		}
		return null;
	}
	
}
