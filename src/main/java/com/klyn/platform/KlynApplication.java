package com.klyn.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KlynApplication {

	public static void main(String[] args) {
		SpringApplication.run(KlynApplication.class, args);
	}

}