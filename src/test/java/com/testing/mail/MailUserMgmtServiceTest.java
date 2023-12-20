
package com.testing.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.testing.mail.rest.MailReportMgmtService;
import com.testing.mail.rest.MailUserMgmtService;
import com.testing.mail.service.DBConnectorService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class MailUserMgmtServiceTest {

	@Autowired
	DBConnectorService dbConnectorService;
	@Autowired
	MailUserMgmtService mailUsermgmtService;
	@Autowired
	MailReportMgmtService report;
	
	
	@Test
	void testMappedUsers() {
		log.warn("STARTED");
		ResponseEntity<?>response =  mailUsermgmtService.getMappedUsers("63dcf00ee658df30472a642a", "63dcf008e658df30472a6428", "63bd7499ec4e92f179248c54", 0, 20);
		log.warn("ENDED");
		if(response!=null) {
			System.out.println((String)response.getBody());
		}
	}
	@Test
	void testJobs() {
		ResponseEntity<?>response =mailUsermgmtService.getEmailJobs("63e364fbf76e1d709bf00a11", 0, 20);
		log.warn("ENDED");
		if(response!=null) {
			System.out.println((String)response.getBody());
		}
	}
	
	@Test
	void testWorkSpaces() {
		ResponseEntity<?>response =mailUsermgmtService.getWorkSpacesByJob("63e39499826606360f500e17","63e364fbf76e1d709bf00a11", 0, 20);
		log.warn("ENDED");
		if(response!=null) {
			System.out.println(response.getBody().toString());
		}
	}
	
	@Test
	void testCsvUpload() {
		try {
			File csvFile = new File("C:\\Users\\BrahmaiahG\\Desktop\\test\\test didgnet.csv");
			ResponseEntity<?>response = mailUsermgmtService.mapUsersBasedOnCsv("64d57bea0f96f44335123f95", new FileInputStream(csvFile), "65657145bfe3fb7188a4a528", "653a537383e0ac5c1a475337");
			log.warn("ENDED");
			if(response!=null) {
				System.out.println(response.getBody().toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	void testDownloadValidCsv() {
		try {
			ResponseEntity<?>response = mailUsermgmtService.downloadValidateCsv("64d57bea0f96f44335123f95", 359);
			log.warn("ENDED");
			if(response!=null) {
				File f = File.createTempFile("emailCsv", ".csv");
				InputStream stream = (InputStream)response.getBody();
				IOUtils.copy(stream, new FileOutputStream(f));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Test
	void testReport() {
		ResponseEntity<?>response = report.getPremigartionForUser("granger@cloudfuze.us", "63e38fd8826606360f500e16", "GMAIL");
		if(response!=null) {
			System.out.println(response.getBody().toString());
		}
	}
	@Test
	void testAdminClouds() {
		mailUsermgmtService.getAdminCloudsByUser("64ae920cdd9631217b1a9b95");
	}
	@Test
	void testMembersDownloading() {
		try {
			mailUsermgmtService.downloadUsers("644110c0b6da481dcc00a4ec", "6577d908897de034d23c8232");
		} catch (JsonProcessingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
