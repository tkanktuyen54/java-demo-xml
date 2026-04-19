package com.bigproject.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI bigProjectOpenApi() {
		return new OpenAPI().info(new Info().title("Big Project API").description("REST API").version("v1"));
	}

}
