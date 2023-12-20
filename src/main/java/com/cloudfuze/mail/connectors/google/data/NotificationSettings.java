package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class NotificationSettings {
	@SerializedName("notifications")
	@Expose
	private List<Notification> notifications;
}
