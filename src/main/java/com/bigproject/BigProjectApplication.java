package com.bigproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Quét toàn bộ com.bigproject (core, modules, common)
public class BigProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(BigProjectApplication.class, args);
	}

}
