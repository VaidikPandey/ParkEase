package com.parkease.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling; //  It activates Spring's scheduled task support — needed for the auto-cancel expired bookings job

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class BookingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}
}