package com.testing.mail.connectors.google.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ForwardSettings {
	@SerializedName("forwardingAddresses")
	@Expose
	private List<ForwardingAddress> forwardingAddresses;
}
