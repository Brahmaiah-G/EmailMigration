package com.cloudfuze.mail.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new AbstractHttpMessageConverter<FileInputStream>(MediaType.APPLICATION_OCTET_STREAM) {
            protected boolean supports(Class<?> clazz) {
                return FileInputStream.class.isAssignableFrom(clazz);
            }

            protected FileInputStream readInternal(Class<? extends FileInputStream> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
                return (FileInputStream) inputMessage.getBody();
            }

            protected void writeInternal(FileInputStream inputStream, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
                IOUtils.copy(inputStream, outputMessage.getBody());
            }
        });
        super.configureMessageConverters(converters);
    }
}
