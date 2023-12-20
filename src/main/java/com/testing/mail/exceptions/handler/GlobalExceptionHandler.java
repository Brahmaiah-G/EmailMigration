package com.testing.mail.exceptions.handler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.testing.mail.exceptions.DuplicationException;
import com.testing.mail.exceptions.InvaildCredentialsException;
import com.testing.mail.exceptions.InvalidTokenException;
import com.testing.mail.exceptions.MailMigrationException;
import com.testing.mail.exceptions.ProcessingException;
import com.testing.mail.exceptions.TokenExpiredException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleDefaultException(Exception exception) {   
    	log.error(ExceptionUtils.getStackTrace(exception));
        return new ResponseEntity<>(ErrorResponse.builder().
					message(exception.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);        
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleDefaultRuntimeException(RuntimeException exception) {  
    	log.error(ExceptionUtils.getStackTrace(exception));
        return new ResponseEntity<>(ErrorResponse.builder().
					message(exception.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);        
    }
    
    @ExceptionHandler(value = InvaildCredentialsException.class)
    public ResponseEntity<?> handleInvaildCredentialsException(InvaildCredentialsException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
        return new ResponseEntity<>(ErrorResponse.builder().
        						message(exception.getMessage()).build(), HttpStatus.UNAUTHORIZED);
       
    }
    
    @ExceptionHandler(value = MailMigrationException.class)
    public ResponseEntity<?> handleUserNotFoundExceptionn(MailMigrationException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
    	return new ResponseEntity<>(ErrorResponse.builder().
    			message("User Does Not Exists").build(), HttpStatus.NOT_FOUND);
    	
    }
    
    @ExceptionHandler(value = InvalidTokenException.class)
    public ResponseEntity<?> handleInvalidTokenException(InvalidTokenException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
    	return new ResponseEntity<>(ErrorResponse.builder().
    			message("Inavlid Token!, Please Re-Login").build(), HttpStatus.UNAUTHORIZED);
    	
    }
    @ExceptionHandler(value = ProcessingException.class)
    public ResponseEntity<?> handleProcessingException(ProcessingException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
    	return new ResponseEntity<>(ErrorResponse.builder().
    			message("Inavlid Token!, Please Re-Login").build(), HttpStatus.UNAUTHORIZED);
    	
    }
    @ExceptionHandler(value = TokenExpiredException.class)
    public ResponseEntity<?> handleTokenExpiredException(TokenExpiredException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
    	return new ResponseEntity<>(ErrorResponse.builder().
    			message("Inavlid Token!, Please Re-Login").build(), HttpStatus.UNAUTHORIZED);
    	
    }
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
    	return new ResponseEntity<>(ErrorResponse.builder().
    			message("Not Authorized to access this resource").build(), HttpStatus.UNAUTHORIZED);
    	
    }
    
    @ExceptionHandler(value = DuplicationException.class)
    public ResponseEntity<?> handleUserNotFoundExceptionn(DuplicationException exception) {
    	log.error(ExceptionUtils.getStackTrace(exception));
    	return new ResponseEntity<>(ErrorResponse.builder().
    			message("Resource already exists").build(), HttpStatus.CONFLICT);
    	
    }

}