package com.cloudfuze.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConferenceProperties {
	@SerializedName("allowedConferenceSolutionTypes")
	@Expose
	private List<String> allowedConferenceSolutionTypes;
}
