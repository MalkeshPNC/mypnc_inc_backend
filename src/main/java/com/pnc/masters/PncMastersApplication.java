package com.pnc.masters;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class PncMastersApplication {

    public static void main(String[] args) {
        SpringApplication.run(PncMastersApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
