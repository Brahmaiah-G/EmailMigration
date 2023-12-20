package com.cloudfuze.mail.repo.entities;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document("ThreadControl")
@TypeAlias("ThreadControl")
@Getter
@Setter
public class ThreadControl {

	private boolean active;
	private int moveCount;
	private int retryCount;
	private int pickCount;
	private int changesCount;
	private int metadataCount;
	private int workSpaceUpdates;
	private int calendarCount;
	private boolean stopPicking;
	private boolean stopMoving;
	private boolean stopUpdating;
	private boolean stopChanges;
	private boolean metadata;
	private boolean stopCalendar;
	private long pauseMoreThanThis;
	private boolean stopPause;
	private long batchLimit = 60;
	private boolean stopPurge;
	
}
