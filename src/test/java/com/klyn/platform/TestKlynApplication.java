package com.klyn.platform;

import org.springframework.boot.SpringApplication;

public class TestKlynApplication {

	public static void main(String[] args) {
		SpringApplication.from(KlynApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}