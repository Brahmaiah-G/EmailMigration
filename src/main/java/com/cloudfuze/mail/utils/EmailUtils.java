package com.cloudfuze.mail.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.lang3.exception.ExceptionUtils;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailUtils {


	public static StringBuilder TO =new StringBuilder("To");
	public static StringBuilder FROM = new StringBuilder("From");
	public static StringBuilder CC = new StringBuilder("Cc");
	public static StringBuilder BCC = new StringBuilder("Bcc");
	public static StringBuilder REPLY_TO= new StringBuilder("Reply-To");
	public static StringBuilder SUBJECT = new StringBuilder("Subject:");
	public static StringBuilder DATE = new StringBuilder("Date: ");
	public static StringBuilder MIME_VERSION = new StringBuilder("MIME-Version: 1.0");
	public static StringBuilder CONTENT_TYPE = new StringBuilder("Content-Type");
	public static StringBuilder CONTENT_DISPOSITION = new StringBuilder("Content-Disposition: ");
	public static StringBuilder ATTACHMENT =new StringBuilder("attachment");
	public static StringBuilder CONTENT_TRANSFER_ENCODING =new StringBuilder( "Content-Transfer-Encoding: base64");
	public static StringBuilder BOUNDARY = new StringBuilder("boundary=");

	
	public static final String ACCEPTED = "accepted";
	public static final String NEEDS_ACTION = "needsAction";
	public static final String DECLINED = "declined";

	public static String encodeFileToBase64Binary(InputStream inputStream){
		String encodedfile = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = inputStream.read(buffer)) > -1 ) {
				baos.write(buffer, 0, len);
			}
			baos.flush();
			encodedfile = new String(Base64.getEncoder().encode(baos.toByteArray()), "UTF-8");
		} catch (FileNotFoundException e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}

		return encodedfile;
	}

	public static StringBuffer encodeTextToBase64Binary(Object message) {

		try {
			return new StringBuffer(new String(Base64.getUrlEncoder().encode(message.toString().getBytes()), StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}


	public static StringBuffer encodeMimeTextToBase64Binary(String message) {

		try {
			return new StringBuffer(new String(org.apache.commons.codec.binary.Base64.encodeBase64(message.getBytes("utf-8"))));
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}
	
	
	public enum EmailKeys{
		UNREAD,DRAFT,INBOX,SENT,STARRED,drafts;
	}

}
