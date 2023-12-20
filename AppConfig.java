package com.cloudfuze.mail.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.cloudfuze.mail.connectors.scheduler.CalendarMetadataScheduler;
import com.cloudfuze.mail.connectors.scheduler.CalendarMigrationScheduler;
import com.cloudfuze.mail.connectors.scheduler.CalendarPickerScheduler;
import com.cloudfuze.mail.connectors.scheduler.EventInstanceUpdateScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailChangesScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailContactsScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailDraftMigrationScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailDraftsCreationcheduler;
import com.cloudfuze.mail.connectors.scheduler.MailMigrationSchedulerV2;
import com.cloudfuze.mail.connectors.scheduler.MailPickingScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailPurgerScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailRulesScheduler;
import com.cloudfuze.mail.connectors.scheduler.MailSettingsScheduler;
import com.cloudfuze.mail.connectors.scheduler.MetadataUpdateScheduler;

import lombok.Getter;
import lombok.Setter;

@Setter@Getter
@Configuration
@EnableScheduling
public class AppConfig {
	private static int SOCKET_TIMEOUT_IN_MS = 10 * 6000;
	@Bean
	public RestTemplate restTemplate() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(SOCKET_TIMEOUT_IN_MS)
				.setSocketTimeout(SOCKET_TIMEOUT_IN_MS)
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(500);
		connectionManager.setDefaultMaxPerRoute(20);

		CloseableHttpClient httpClient = HttpClientBuilder
				.create()
				.setDefaultRequestConfig(requestConfig)
				.setConnectionManager(connectionManager)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		return new RestTemplate(requestFactory);
	}
	
	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2A,10);
	}
	
	@Value("${com.cloudfuze.threads.core}")
	int corePoolSize;
	@Value("${com.cloudfuze.threads.max}")
	int maxPoolSize;
	@Value("${com.cloudfuze.threads.queue}")
	int queue;
	
	@Bean(name ="migrationScheduler")
	public ThreadPoolTaskExecutor getMigrationThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailMigrationScheduler");
		taskExecutor.setThreadGroupName("mailMigrationScheduler");
		return taskExecutor;
	}
	
	@Bean(name ="ruleSettingContactsScheduler")
	public ThreadPoolTaskExecutor getRSCThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("ruleSettingContactsScheduler");
		taskExecutor.setThreadGroupName("ruleSettingContactsScheduler");
		return taskExecutor;
	}
	
	@Bean(name ="draftMigrationScheduler")
	public ThreadPoolTaskExecutor getDraftMigrationThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailDraftMigrationScheduler");
		taskExecutor.setThreadGroupName("mailDraftMigrationScheduler");
		return taskExecutor;
	}
	
	@Bean(name ="metadataUpdateScheduler")
	public ThreadPoolTaskExecutor getMetadataThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailDraftMigrationScheduler");
		taskExecutor.setThreadGroupName("mailDraftMigrationScheduler");
		return taskExecutor;
	}
	@Bean(name ="calendarMetadataUpdateScheduler")
	public ThreadPoolTaskExecutor getCalendarMetadataThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailDraftMigrationScheduler");
		taskExecutor.setThreadGroupName("mailDraftMigrationScheduler");
		return taskExecutor;
	}
	
	
	@Bean(name ="draftCreationScheduler")
	public ThreadPoolTaskExecutor getDraftCreationThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailDraftCreationScheduler");
		taskExecutor.setThreadGroupName("mailDraftCreationScheduler");
		return taskExecutor;
	}
	
	@Bean(name ="pickingScheduler")
	public ThreadPoolTaskExecutor getPickingTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailPickingScheduler");
		taskExecutor.setThreadGroupName("mailPickingScheduler");
		return taskExecutor;
	}
	
	@Bean(name ="changesScheduler")
	public ThreadPoolTaskExecutor getAttachmentsThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setCorePoolSize(corePoolSize);
		taskExecutor.setMaxPoolSize(maxPoolSize);
		taskExecutor.setQueueCapacity(queue);
		taskExecutor.setThreadNamePrefix("mailchangesScheduler");
		taskExecutor.setThreadGroupName("mailchangesScheduler");
		return taskExecutor;
	}
	
//	@Bean
//	public MailMigrationScheduler getMailMigrationScheduler() {
//		return new MailMigrationScheduler(getMigrationThreadPoolTaskExecutor());
//	}
	
//	@Bean
//	public MailMigrationSchedulerV2 getMailMigrationScheduler() {
//		return new MailMigrationSchedulerV2(getMigrationThreadPoolTaskExecutor());
//	}
//	/**
//	 * Batch Request Schedulers
//	*/
//	@Bean
//	public MailDraftsCreationcheduler getMailDraftsCreationcheduler() {
//		return new MailDraftsCreationcheduler(getDraftCreationThreadPoolTaskExecutor());
//	}
//	
//	@Bean
//	public MailDraftMigrationScheduler getMailDraftMigrationScheduler() {
//		return new MailDraftMigrationScheduler(getDraftMigrationThreadPoolTaskExecutor());
//	}
//	
//	@Bean
//	public MetadataUpdateScheduler getMetadataUpdateScheduler() {
//		return new MetadataUpdateScheduler(getMetadataThreadPoolTaskExecutor());
//	}
//	
//	
//	@Bean
//	public MailPickingScheduler getMailPickerScheduler() {
//		return new MailPickingScheduler(getPickingTaskExecutor());
//	}
//	
//	@Bean
//	public MailChangesScheduler getMailChangesScheduler() {
//		return new MailChangesScheduler(getAttachmentsThreadPoolTaskExecutor());
//	}
//////	
//////	CALENDAR BEANS DECLARATION
//////	
//	
	@Bean
	public CalendarPickerScheduler getCalendarPickerScheduler() {
		return new CalendarPickerScheduler(getPickingTaskExecutor());
	}
//	
////	@Bean
////	@Profile("picking")
////	public CalendarChangesScheduler getCalendarChangesScheduler() {
////		return new MailChangesScheduler(getAttachmentsThreadPoolTaskExecutor());
////	}
//	
//	@Bean
	public CalendarMigrationScheduler getCalendarMigrationScheduler() {
		return new CalendarMigrationScheduler(getMigrationThreadPoolTaskExecutor());
	}
//	
//	@Bean
//	public CalendarMetadataScheduler getCalendarMetadataUpdateScheduler() {
//		return new CalendarMetadataScheduler(getCalendarMetadataThreadPoolTaskExecutor());
//	}
//	
//	/**
//	 *Email Purger 
//	*/
//	
//	@Bean
//	public MailPurgerScheduler getMailPurgerScheduler() {
//		return new MailPurgerScheduler(getAttachmentsThreadPoolTaskExecutor());
//	}
//	/**
//	 * EventsInstanceUpdateScheduler
//	*/
//	@Bean	
//	public EventInstanceUpdateScheduler eventsInsatnceUpdateScheduler() {
//		return new EventInstanceUpdateScheduler(getPickingTaskExecutor());
//	}
//	/**
//	 * Rules,Contacts,Settings Scheduler
//	*/
//	@Bean
//	public MailRulesScheduler getMailRulesScheduler() {
//		return new MailRulesScheduler(getRSCThreadPoolTaskExecutor());
//	}
//	
//	@Bean
//	public MailContactsScheduler getMailContactsScheduler() {
//		return new MailContactsScheduler(getRSCThreadPoolTaskExecutor());
//	}
//	
//	@Bean
//	public MailSettingsScheduler getMailSettingsScheduler() {
//		return new MailSettingsScheduler(getRSCThreadPoolTaskExecutor());
//	}
	
	@Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(10);
        pool.setMaxPoolSize(15);
        pool.setQueueCapacity(100);
        pool.setWaitForTasksToCompleteOnShutdown(true);
        return pool;
    }
	
	
}