package com.cloudfuze.mail.emailUtility;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudfuze.mail.repo.entities.Clouds;
import com.cloudfuze.mail.repo.entities.EmailJobDetails;


@Component
public class EmailContentCreatorUtil {

	@Value("${base.staging.url}")
	String baseURL;
	
	//Email Subjects
	static final String RESGISTRATION_SUCCESS_SUBJECT="Registration Successful";
	static final String EMAIL_MIGRATION_REPORT_SUBJECT="Email Migration Report";
	static final String EMAIL_MIGRATION_TIME_REPORT_SUBJECT="Email Migration Time Report";
	static final String PASSWORD_RESET_SUBJECT = "Password Reset Requested";
	static final String LICENSE_NOTIFICATION	= "Remainder for your License Renewal";

	public String constructRegistrationSuccessBody(String userName,String ent) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("Hi, ").append(userName.toUpperCase()).append("<br/>");
		String host = ent;
		try {
			URI url = new URI(baseURL);
			if(url!=null) {
				host = url.getHost();
			}
		} catch (URISyntaxException e) {
		}
		String _baseURL = baseURL.replace(host, ent);
		buffer.append("<h1> Registration Successful on CloudFuze Connect</h1>")
		.append("<br/> <br/> <br/>")
		.append("<a href=\"")
		.append(_baseURL)
		.append("\"><h3>Login</h3></a>")
		.append("<br/> <br/> <br/>")
		.append("<p style=\"font-style: italic;\">CloudFuze, LLC <br/><br/></p>")
		.append("<img src=\"https://www.cloudfuze.com/wp-content/uploads/2020/12/logo.svg\" style=\"margin-left:auto;margin-right: auto;\"/><br/>")
		.append("<small>© CloudFuze, Inc. 2022 All rights reserved</small><br/>");	 

		return buffer.toString();
	}




	public String generateReport(String userName) {

		return new StringBuffer().append("Hi, ").append(userName).append("<br/>")
				.append("<h1> Your Email Migration Report Attached Here</h1>")
				.append("<br/>")
				.append("For more details Please login<a href=\"")
				.append(baseURL)
				.append("\"><h3>Login</h3></a>")
				.append("<br/>")
				.append("<p style=\"font-style: italic;\">CloudFuze, LLC <br/><br/></p>")
				.append("<img src=\"https://www.cloudfuze.com/wp-content/uploads/2020/12/logo.svg\" style=\"margin-left:auto;margin-right: auto;\"/><br/>")
				.append("<small>© CloudFuze, Inc. 2022 All rights reserved</small><br/>")
				.toString();	 

	}

	/**
	 * For Generating the TimeReport of how much time Taken for completing the email migration report
	*/
	public String generateTimeReport(EmailJobDetails emailWorkSpace) {
		ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/Oslo");
		long created = emailWorkSpace.getCreatedTime().atZone(zoneId).toEpochSecond();
		long endTime = emailWorkSpace.getModifiedTime()==null? LocalDateTime.now().atZone(zoneId).toEpochSecond(): emailWorkSpace.getModifiedTime().atZone(zoneId).toEpochSecond();
		
		return new StringBuffer().
				append("Here the Time Taken for completing the Email Migration ")
				.append("For the User : ").append(emailWorkSpace.getOwnerEmailId()).append("<br><br/>")
				.append("<table border=1>")
				.append("<tbody>")
				.append("<tr>").append("<td>")
				.append("<strong>Job Id</strong></td><td>"+emailWorkSpace.getId()).append("</td>").append("</tr>").append("<tr>").append("<td>")
				.append("<strong>Mapping Pair</strong></td><td>"+emailWorkSpace.getFromMailId()+" TO "+emailWorkSpace.getToMailId()).append("</td>").append("</tr>").append("<tr>").append("<td>")
				.append("<strong> Started on</strong></td><td>"+emailWorkSpace.getCreatedTime()).append("</td>").append("</tr>").append("<tr>").append("<td>")
				.append("<strong>Ended on</td><td>"+emailWorkSpace.getModifiedTime()+"</strong>").append("</td>").append("</tr>").append("<tr>").append("<td>")
				.append("<strong>Total Time Taken</strong></td><td>"+TimeUnit.SECONDS.toDays((endTime-created)) +" Days"+TimeUnit.SECONDS.toHours((endTime-created))+" H,"+TimeUnit.SECONDS.toMinutes((endTime-created))+" M, "+TimeUnit.SECONDS.toSeconds((endTime-created))+" S").append("</td>").append("</tr>")
				.append("</tbody>")
				.append("</table>")
				.append("<p style=\"font-style: italic;\">CloudFuze, LLC <br/><br/></p>")
				.append("<img src=\"https://www.cloudfuze.com/wp-content/uploads/2020/12/logo.svg\" style=\"margin-left:auto;margin-right: auto;\"/><br/>")
				.append("<small>© CloudFuze, Inc. 2022 All rights reserved</small><br/>")
				.toString();	 

	}
	
	
	
	

	public String constructPasswordResetMailBody(String email, String userName, String token) {

		return new StringBuffer().append("Hi, ").append(userName.toUpperCase()).append("<br/>")
				.append("<h1> Password Reset Request Raised for CloudFuze Connect</h1>")
				.append("<br/> <br/> <br/>")
				.append("<a style=\"tex-decoration:none;color:green;\" href=\"")									
				.append(baseURL)
				.append("/pwd/verify/")
				.append(token)
				.append("/user/")
				.append(email)
				.append("\"><h3>Click to Verify and Reset </h3></a>")
				.append("<br/> <br/> <br/>")
				.append("<p style=\"font-style: italic;\">CloudFuze, LLC <br/><br/></p>")						
				.append("<img src=\"https://www.cloudfuze.com/wp-content/uploads/2020/12/logo.svg\" style=\"margin-left:auto;margin-right: auto;\"/><br/>")
				.append("<small>© CloudFuze, Inc. 2022 All rights reserved</small><br/>")
				.toString();
	}	


	public String constructLicenseRenwalNotifyMailBody(Clouds sub) {

		return new StringBuffer().append("<!DOCTYPE html> <body>").append("<center>").append("Hi, ").append(sub.getAdminEmailId().split("@")[0].toUpperCase())
				.append("<h3> This is a Simple Remainder Regarding your License Renewal </h3>")
				.append("</center>").append("<p style=\" font-style: italic;\"> Your Lincese is about to expiry for the <b>"+sub.getCloudName()+"</b> on <b>"+""+"</b></p>")
				.append("<center>")
				.append("<a style=\"tex-decoration:none;color:green;\" href=\"")									
				.append(baseURL+">")
				.append("<p style=\"font-style: italic;\">CloudFuze, LLC </p></a>")	
				.append("<br></br>")
				.append("<img src=\"https://www.cloudfuze.com/wp-content/uploads/2020/12/logo.svg\" style=\"margin-left:auto;margin-right: auto;\"/>")
				.append("<br></br>")
				.append("<small>© CloudFuze, Inc. 2022 All rights reserved</small>")
				.append("</center></body>")
				.toString();
	}	





}