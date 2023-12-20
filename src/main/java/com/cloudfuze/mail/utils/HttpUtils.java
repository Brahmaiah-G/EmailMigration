package com.cloudfuze.mail.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

import com.cloudfuze.mail.repo.entities.ThreadControl;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpUtils {
	
	public enum ERROR_REASONS{
		CLOUDS_NOT_AVAILABLE,FAILURE,CAN_T_ABLETO_INITIATE,USER_OR_EMAIL_NOT_AVAILABLE,NO_CONTETNT_TO_PROCEED;
	}
	public enum SUCESS_REASONS{
		INITIATED,SUCESS;
	}
	
	public static ResponseEntity<Object> Ok(Object body){
		if(ObjectUtils.isEmpty(body)) {
			return ResponseEntity.ok().build();
		}else {
			return ResponseEntity.ok(body);
		}
	}
	
	public static ResponseEntity<Object> Ok(Object body,long count){
		if(ObjectUtils.isEmpty(body)) {
			return ResponseEntity.ok().header("total", ""+count).build();
		}else {
			return ResponseEntity.accepted().header("total", ""+count).body(body);
		}
	}
	
	public static ResponseEntity<InputStreamResource> buildStreamOutResponse(InputStream inputStream,String fileName) throws JsonProcessingException {
		InputStreamResource resource = new InputStreamResource(inputStream);
		return ResponseEntity.status(201).contentType(MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename="
				+ fileName).body(resource);

	}
	
	private HttpUtils() {
	}
	

	public static ResponseEntity<Object> BadRequest(Object body){
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	public static ResponseEntity<Object> NotFound(Object body){
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}
	
	public static boolean checkController(ThreadControl threadControl) {
		if(threadControl==null || !threadControl.isActive()) {
			log.warn("**********Thread Control is not ACTIVE or not CREATED check");
			return true;
		}
		return false;
	}
	
	/**
	 * For Getting the Anchor tags in the gmail migration for largeFile upload
	*/
	public static Map<String,String>getAnchorTags(String html){
		if(StringUtils.isEmpty(html)) {
			return null;
		}
		Reader reader = new StringReader(html);
		HTMLEditorKit.Parser parser = new ParserDelegator();
		Map<String,String>links = new HashMap<String, String>();

		try {
			parser.parse(reader, new HTMLEditorKit.ParserCallback(){
				public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
					if(t == HTML.Tag.A) {
						Object link = a.getAttribute(HTML.Attribute.HREF);
						Object name = a.getAttribute("aria-label");
						if(link != null) {
							links.put(String.valueOf(link),String.valueOf(name));
						}
					}
				}
			}, true);
			reader.close();
		} catch (IOException e) {
		}
		return links;
	}
	
	
	/**
	 * For Getting the Anchor tags in the gmail migration for largeFile upload
	*/
	public static String createAnchorTags(String html,String url,String name){

		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(html);
			org.jsoup.nodes.Element div = doc.getElementsByTag("body").get(0);
			div.append("<a href="+url+">"+name+"</a>");
			return doc.html();
		} catch (Exception e) {
			log.warn(ExceptionUtils.getStackTrace(e));
		}
		return html; 
	}

	
}

