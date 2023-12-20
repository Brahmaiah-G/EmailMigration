package com.cloudfuze.mail;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.test.context.SpringBootTest;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;

@SpringBootTest
public class ExchangeServices {

	public static void main(String[] args) {
		ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
		try {
			service.setUrl(new URI("https://outlook.office365.com/EWS/exchange.asmx"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//service.createItem(item, parentFolderId, MessageDisposition.SaveOnly, SendInvitationsMode.SendToNone);
		//ExchangeCredentials credentials = ExchangeCredentials.getExchangeCredentialsFromNetworkCredential(userName, password, domain);
	}
}
