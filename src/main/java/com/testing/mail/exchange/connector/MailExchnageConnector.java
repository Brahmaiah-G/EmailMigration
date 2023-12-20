package com.testing.mail.exchange.connector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.service.MessageDisposition;
import microsoft.exchange.webservices.data.core.enumeration.service.SendInvitationsMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.misc.ITraceListener;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.Mailbox;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;
/**
 * Entry point.
 * 
 * @param args
 * @throws Exception
 */
public class MailExchnageConnector {
	public static void main(String[] args) throws Exception {

		// Pro tip: make sure to set your proxy configuration here if needed 
		// and exclude outlook.office365.com from proxy SSL inspection.

		String clientId = "dc94adef-3f09-488d-9734-c55b96e614cc";
		String clientSecret = "id08Q~TQL5PlIImBlaOz4DD8M84qjwd-4QIvwcVF";
		String tenantName = "common";
		String recipientAddr = "brahmaiah@cloudfuze.com";
		String senderAddress = "granger@gajha.com";

		ITraceListener traceListener = new ITraceListener() {

			@Override
			public void trace(String traceType, String traceMessage) {
				// TODO log it, do whatever...

			}
		};
		System.out.println();
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		// I used a ManagedExecutorService provided by glassfish but you can 
		// use an ExecutorService and manage it yourself.
		String token = MsEwsTokenProvider.getAccesToken(clientId, clientSecret, executorService);
		// don't log this in production!
		System.out.println("token=" + token);

		// test mailbox read access
		System.out.println("geting emails");
		try (ExchangeService service = MsEwsTokenProvider.getAuthenticatedService(token, senderAddress)) {
			//service.createItem(new Item(service), new FolderId("inbox"), MessageDisposition.SaveOnly, SendInvitationsMode.SendToNone);
			listInboxMessages(service, senderAddress);
		}

		// send a message
		System.out.println("sending a message");
		try (ExchangeService service =MsEwsTokenProvider.getAuthenticatedService(token, senderAddress)) {
			sendTestMessage(service, recipientAddr, senderAddress);
		}

		System.out.println("finished");
	}

	public static void sendTestMessage(ExchangeService service, String recipientAddr, String senderAddr)
			throws Exception {
		EmailMessage msg = new EmailMessage(service);
		msg.setSubject("Hello world!");
		msg.setBody(MessageBody.getMessageBodyFromText("Sent using the EWS Java API."));
		msg.getToRecipients().add(recipientAddr);
		msg.send();
		msg.setSender(new EmailAddress(senderAddr));
	}

	public static void listInboxMessages(ExchangeService service, String mailboxAddr) throws Exception {
		ItemView view = new ItemView(50);
		Mailbox mb = new Mailbox(mailboxAddr);
		FolderId folder = new FolderId(WellKnownFolderName.Inbox, mb);
		FindItemsResults<Item> result = service.findItems(folder, view);
		result.forEach(i -> {
			try {
				System.out.println("subject=" + i.getSubject());
			} catch (ServiceLocalException e) {
				e.printStackTrace();
			}
		});
	}
}
