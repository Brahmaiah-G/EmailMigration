package com.cloudfuze.mail.repo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.Getter;
import lombok.Setter;

@Service
@Setter@Getter
public class MongoDbUtil {

	@Value("${spring.data.mongodb.database}")
	private String database ;
	
	@Value("${spring.data.mongodb.uri}")
	private String mongoClient ;
	
	@Bean(name="connectMongoClient")
	public MongoClient mongoClient() {
		return MongoClients.create(mongoClient);
	}
	
    @Bean(name="mongoTemplate")
    public MongoTemplate mongoTemplate() {
    	
        return new MongoTemplate(mongoClient(), database);
    }
	
	
}
