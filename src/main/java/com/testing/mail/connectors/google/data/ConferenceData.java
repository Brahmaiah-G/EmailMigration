package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class ConferenceData {
	@SerializedName("entryPoints")
	@Expose
	private List<EntryPoint> entryPoints;
	@SerializedName("conferenceSolution")
	@Expose
	private ConferenceSolution conferenceSolution;
	@SerializedName("conferenceId")
	@Expose
	private String conferenceId;

}
