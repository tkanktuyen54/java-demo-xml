package com.bigproject.modules.employee.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeConfig {
  @Bean
  Clock employeeClock() {
    return Clock.systemUTC();
  }
}
