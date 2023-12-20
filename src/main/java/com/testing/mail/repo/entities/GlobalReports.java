package com.testing.mail.repo.entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(collection = "GlobalReports")
@TypeAlias(value="GlobalReports")
public class GlobalReports {
	@Id
	private String id;
	private List<String>to;
	private String from;
	private List<String>cc;
}
