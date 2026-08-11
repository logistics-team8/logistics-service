package com.logistics.notificationservice;

import com.logistics.notificationservice.infrastructure.security.ServiceTokenProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@EnableFeignClients
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
    @Bean
    CommandLineRunner printServiceToken(
            ServiceTokenProvider serviceTokenProvider
    ) {
        return args -> {
            String token =
                    serviceTokenProvider.createToken(
                            "delivery-service"
                    );

            System.out.println("==============================");
            System.out.println("TEST SERVICE TOKEN");
            System.out.println(token);
            System.out.println("==============================");
        };
    }
}