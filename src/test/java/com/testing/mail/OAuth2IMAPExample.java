package com.testing.mail;

import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.util.MailSSLSocketFactory;


public class OAuth2IMAPExample {

    public static void main(String[] args) throws Exception {
        // Your OAuth 2.0 credentials
        String userEmail = "your-email@gmail.com";
        String accessToken = "your-access-token";
        String clientId = "your-client-id";
        String clientSecret = "your-client-secret";
        String refreshToken = "your-refresh-token";

        // IMAP properties
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.ssl.enable", "true");
        props.setProperty("mail.imap.auth.mechanisms", "XOAUTH2");

        // Use the OAuth2 authenticator
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userEmail, accessToken);
            }
        };

        Session session = Session.getInstance(props, authenticator);

        // Connect to the IMAP server
        Store store = connectToImap(session, "imap.gmail.com", userEmail, accessToken);

        // Create and send the email
        sendMessage(session, store, userEmail);

        // Disconnect from the IMAP server
        store.close();
    }

    private static Store connectToImap(Session session, String host, String userEmail, String accessToken) throws MessagingException, GeneralSecurityException {
        // Create a custom SSL socket factory to handle OAuth2
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
       // sf.setSocketFactory(SSLSocketFactory.getDefault());

        // Set the OAuth2 token
        session.getProperties().put("mail.imaps.sasl.enable", "true");
        session.getProperties().put("mail.imaps.sasl.mechanisms", "XOAUTH2");
        session.getProperties().put("mail.imaps.sasl.authorizationid", userEmail);
        session.getProperties().put("mail.imaps.sasl.token", accessToken);
        session.getProperties().put("mail.imaps.ssl.socketFactory", sf);

        // Connect to the IMAP server
        Store store = new IMAPSSLStore(session, new URLName("imap", host, 993, null, userEmail, accessToken));
        store.connect(host, userEmail, accessToken);

        return store;
    }

    private static void sendMessage(Session session, Store store, String userEmail) throws MessagingException {
        // Create a MimeMessage
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(userEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("recipient@example.com"));
        message.setSubject("Test Email");
        message.setText("This is a test email.");

        // Send the email
        Transport.send(message);
    }
}


