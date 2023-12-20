package com.testing.mail.connectors.management;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.testing.mail.connectors.factory.MailServiceFactory;
import com.testing.mail.dao.entities.EmailFlagsInfo;
import com.testing.mail.exceptions.handler.ThreadExceptionHandler;
import com.testing.mail.repo.entities.Clouds;
import com.testing.mail.repo.entities.EmailInfo;
import com.testing.mail.repo.entities.Clouds.CLOUD_NAME;
import com.testing.mail.repo.impl.CloudsRepoImpl;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CloudMailsUpdaterTask implements Callable<Object> {

		private Clouds vo;
		private String userId;
		private CLOUD_NAME cloudName;
		private MailServiceFactory vendorServiceFactory;
	    private CloudsRepoImpl cloudsRepo;
	    
		public CloudMailsUpdaterTask(Clouds vo, String userId, CLOUD_NAME cloudName,
				MailServiceFactory vendorServiceFactory, CloudsRepoImpl cloudsRepo) {
			this.vo = vo;
			this.userId = userId;
			this.cloudName = cloudName;
			this.vendorServiceFactory = vendorServiceFactory;
			this.cloudsRepo = cloudsRepo;
		}

		@Override
		public Boolean call() {
			log.warn(" +=====ENTERD FOR UPDATING THE MAILFOLDERS FOR THE USER=="+vo.getAdminEmailId());
			long startTime = System.nanoTime();
			boolean success = false;
			long activeUsers = 0;
			Thread.currentThread().setName("SaveVendorTask " + userId + "time : " + new Date(System.nanoTime()));
			Thread.currentThread().setUncaughtExceptionHandler(new ThreadExceptionHandler());
			int pageSize = 100;
			int skip = 0;
			try {
				List<Clouds> msftUsers = null;
				do {
					msftUsers = cloudsRepo.findCloudsByAdminWithPazination(userId, vo.getAdminMemberId(),pageSize,skip);
					log.warn("--total users found in DB---"+(msftUsers!=null?msftUsers.size():0));
					List<Clouds> cloudUsers = new ArrayList<>();
					for (Clouds user : msftUsers) {
						log.warn("==Getting the MailFolder for the user==="+user.getEmail()+"==ADMIN EMAIL=="+vo.getAdminEmailId());
						if (user != null) {
							try {
								EmailFlagsInfo emailFlagsInfo = new EmailFlagsInfo();
								emailFlagsInfo.setCloudId(user.getId());
								EmailInfo inbox= vendorServiceFactory.getConnectorService(cloudName).getLabel(emailFlagsInfo);
								if(inbox!=null) {
									activeUsers=activeUsers+1;
									user.setActive(true);
								}else {
									user.setActive(false);
								}
								cloudUsers.add(user);
							} catch (Exception e) {
								user.setErrorDescription(ExceptionUtils.getStackTrace(e));
								user.setActive(false);
								log.warn(ExceptionUtils.getStackTrace(e));
							}
							if(cloudUsers.size()>20) {
								cloudsRepo.save(cloudUsers);
								cloudUsers.clear();
							}
						}
					}
					cloudsRepo.save(cloudUsers);
					cloudUsers.clear();
					skip = skip+pageSize;
					log.info("Saving users completed in " + (System.nanoTime() - startTime));
					success = true;
				}while(msftUsers!=null && !msftUsers.isEmpty());
			} catch (Exception e) {
				log.error("Error while saving user list" + ExceptionUtils.getStackTrace(e));
				e.printStackTrace();
				return success;
			}
			if(vo!=null) {
				vo.setProvisioned(activeUsers+1L);
				vo.setNonProvisioned(vo.getTotal()-vo.getProvisioned());
				vo.setPicking(false);
				cloudsRepo.save(vo);
			}
			log.info("CloudSaveTask complete in " + (System.nanoTime()-startTime));
			return success;
		}

}
