package com.ticketsale.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class TicketDiscoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketDiscoveryApplication.class, args);
    }
}