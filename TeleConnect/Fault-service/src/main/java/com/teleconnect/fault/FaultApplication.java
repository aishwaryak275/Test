package com.teleconnect.fault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 2.6 / 4.6 — Fault &amp; Service Request Management.
 * Runs on port 8086 with context path /teleConnect.
 */
@SpringBootApplication
public class FaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(FaultApplication.class, args);
    }
}
