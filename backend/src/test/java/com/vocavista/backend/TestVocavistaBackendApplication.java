package com.vocavista.backend;

import org.springframework.boot.SpringApplication;

public class TestVocavistaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(VocavistaBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
