package com.testing.mail.exceptions.handler;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadExceptionHandler implements UncaughtExceptionHandler{

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("A critical error happened in thread " + t.getName() + " at " + t.getClass()
        + " the error is " + e.getMessage()+ " \n Stacktrace: "  + ExceptionUtils.getStackTrace(e));
	}

}
