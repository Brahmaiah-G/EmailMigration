package com.testing.mail.constants;


/**
 * Only Constants Used For the Email Migration
 * Character Constants
*/

public final class Const {
	
	
	private Const() {}
	
	public static final String BLANK="";
	public static final String SPACE=" ";
	public static final String COLON=":";
	public static final String SLASH="/";
	public static final String PIPE="|";
	public static final String ATTHERATE="@";
	public static final String HYPHEN="-";	 
	public static final char cSPACE=' ';
	public static final char cCOLON=':';
	public static final char cSLASH='/';
	public static final char cPIPE='|';
	public static final char cATTHERATE='@';
	public static final char cHYPHEN='-';
	/**
	 *  Security Constants 
	 */
	public static final String BEARER = "Bearer ";
	
	/**
	* Report Folder Constants
	*
	*/
	
	public static final String REPORT_PATH = "emailReports";
	
	/**
	 OUTLOOKMail Size Constants
	*/
	 public static final Long ATTACHMENT_LIMIT = 3*1024*1024L;
	 /**
	 Mail Size Constants
	*/
	 public static final Long GMAIL_ATTACHMENT_LIMIT = 25*1024*1024L;
	/**
	   Recurrence seperators
	 */
	 public static final String HASHTAG = "#";

	 
}


