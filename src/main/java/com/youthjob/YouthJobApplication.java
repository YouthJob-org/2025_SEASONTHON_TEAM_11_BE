package com.youthjob;

import com.youthjob.api.empprogram.config.EmpCentersProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EmpCentersProps.class)
public class YouthJobApplication {

	public static void main(String[] args) {
		SpringApplication.run(YouthJobApplication.class, args);
	}

}
