package com.example.moneytransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Money Transfer System — Spring Boot entry point.
 *
 * {@code @EnableAsync} activates Spring's asynchronous method execution,
 * used by {@link com.example.moneytransfer.service.BigQueryService} to stream
 * analytics events to BigQuery without blocking the HTTP response thread.
 */
@SpringBootApplication
@EnableAsync
public class MoneyTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferApplication.class, args);
    }

}
