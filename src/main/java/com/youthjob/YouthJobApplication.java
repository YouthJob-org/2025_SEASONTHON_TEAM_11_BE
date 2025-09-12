package com.youthjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YouthJobApplication {

	public static void main(String[] args) {
		SpringApplication.run(YouthJobApplication.class, args);
	}

}
