package com.testing.mail.exceptions.handler;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor@Builder@AllArgsConstructor
public class ErrorResponse implements Serializable {

	private static final long serialVersionUID = -3383482650354066076L;
	
	@Getter@Setter
	private String message;
	
}