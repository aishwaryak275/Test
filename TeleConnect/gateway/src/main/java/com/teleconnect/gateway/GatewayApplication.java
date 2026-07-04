package com.teleconnect.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TeleConnect API Gateway.
 *
 * Single entry point (port 8080) that routes incoming requests to the
 * appropriate downstream microservice based on the URL path prefix:
 *
 *   /teleConnect/iam/**      -> IAM service        (8081)
 *   /teleConnect/api/**      -> Subscriber service (8082)
 *   /teleConnect/plan/**     -> Plan service       (8083)
 *   /teleConnect/usage/**    -> Usage service      (8084)
 *   /teleConnect/billing/**  -> Billing service    (8085)
 *
 * Routes are declared in application.yml.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
