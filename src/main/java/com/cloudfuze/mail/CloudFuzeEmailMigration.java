package com.cloudfuze.mail;

/**
 * @author BrahmaiahG
 *
*/
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("com.cloudfuze.*")
@EnableScheduling
public class CloudFuzeEmailMigration {

	public static void main(String[] args) {
		// System.setProperty("java.net.debug", "true");
		//Enable it for checking the internal debugging
		SpringApplication.run(CloudFuzeEmailMigration.class, args);
	}
	
	// #Schedulers in AppConfig.java 
	// MailServiceFactory.java interface Parent class for connectors(Gmail,Outlook)
	
	
}