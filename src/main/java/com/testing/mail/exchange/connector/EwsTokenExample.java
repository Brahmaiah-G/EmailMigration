package com.testing.mail.exchange.connector;

import microsoft.exchange.webservices.data.autodiscover.AutodiscoverService;
import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.request.HttpWebRequest;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;

public class EwsTokenExample {

	public static void main(String[] args) throws Exception {
//		  String clientId = "6d9ebd36-c62d-4e13-bc20-a44a7ec8122b";
//		    String clientSecret = "id08Q~TQL5PlIImBlaOz4DD8M84qjwd-4QIvwcVF";
//
//		String authority = "https://login.microsoftonline.com/common/oauth2/token"; // Update with your Azure AD authority URL
//		String resource = "https://outlook.office365.com"; // This is the resource URL for Exchange Online
//
//		// Create an instance of AutodiscoverService
//		AutodiscoverService autodiscoverService = new AutodiscoverService();
//
//		// Implement a callback for redirection URL validation
//		autodiscoverService.setRedirectionUrlValidationCallback(new IAutodiscoverRedirectionUrl() {
//			@Override
//			public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
//				return true; // Allow any redirection URL for simplicity, but you can implement your own logic.
//			}
//		});
//
//		// Authenticate using client credentials and obtain a bearer token
//		autodiscoverService.setCredentials(new WebCredentials(clientId, clientSecret));
//		autodiscoverService.autodiscoverRedirectionUrlValidationCallback("granger@gajha.com");
//
//		// Create an instance of ExchangeService and set the access token
//		ExchangeService exchangeService = new ExchangeService();
//		exchangeService.setUrl(new java.net.URI("https://outlook.office365.com/EWS/Exchange.asmx"));
//		exchangeService.setCredentials(new ExchangeCredentials() {
//			@Override
//			public void prepareWebRequest(HttpWebRequest request) {
//				request.getHeaders().put("Authorization", "Bearer " + autodiscoverService.toString());
//			}
//		});
		main();

		// Now you can use the ExchangeService to make requests to the Exchange server with the bearer token.
		// For example, you can use exchangeService to access mailbox data.
	}
	
	public static void main() {
        try {
            // Replace with your email address
            String emailAddress = "granger@gajha.com";

            // Create an instance of ExchangeService
            ExchangeService service = new ExchangeService();
            // Attempt Autodiscover
            service.autodiscoverUrl(emailAddress, new RedirectionUrlCallback());
            System.out.println("Autodiscover response: " + service.getInboxRules("brahmaiah@cloudfuze.com"));

            // Output the discovered EWS URL
            System.out.println("Discovered EWS URL: " + service.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
	  private static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
	        @Override
	        public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
	            // Allow all redirection URLs, even if they are potentially insecure
	            return true;
	        }
	    }
}

