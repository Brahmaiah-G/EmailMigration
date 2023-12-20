package com.testing.mail;

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
public class ApplicationdbMongoUtil {

	@Value("${secondary.database.name}")
	private String database ;
	
	@Value("${secondary.database.uri}")
	private String mongoClient ;

	@Bean(name = "appMongo")
	public MongoClient mongoClient() {
		return MongoClients.create(mongoClient);
	}
	
    @Bean(name="appMongoTemplate")
    public MongoTemplate mongoTemplate() {
    	return new MongoTemplate(mongoClient(), database);

    }
}
