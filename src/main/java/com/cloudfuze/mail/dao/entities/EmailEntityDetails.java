package com.cloudfuze.mail.dao.entities;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmailEntityDetails {

	private List<EmailFlagsInfo> flagsInfo;
	private String nextPageToken;
	private long pageNo;
}
