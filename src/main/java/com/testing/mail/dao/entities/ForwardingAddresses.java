package com.testing.mail.dao.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ForwardingAddresses {
	private String forwardingEmail;
	private String verificationStatus;
}
