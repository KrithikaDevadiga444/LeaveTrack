package com.leavetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeavetrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeavetrackApplication.class, args);
	}

}
