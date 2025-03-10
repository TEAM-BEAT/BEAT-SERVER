package com.beat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class BeatApplication {

	/**
	 * Main entry point for the BeatApplication Spring Boot application.
	 *
	 * <p>This method bootstraps the application by invoking SpringApplication.run
	 * with the BeatApplication class and the provided command-line arguments.</p>
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(BeatApplication.class, args);
	}

}
