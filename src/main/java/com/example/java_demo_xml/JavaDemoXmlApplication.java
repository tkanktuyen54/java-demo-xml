package com.example.java_demo_xml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Bật Spring Boot: cấu hình tự động, quét bean trong package này và con
public class JavaDemoXmlApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaDemoXmlApplication.class, args); // Khởi động context, nạp application.yaml, chạy web server (mặc định 8080)
	}

}
