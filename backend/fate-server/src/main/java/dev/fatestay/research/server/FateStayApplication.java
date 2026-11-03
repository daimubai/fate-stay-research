package dev.fatestay.research.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.fatestay.research")
public class FateStayApplication {
    public static void main(String[] args) {
        SpringApplication.run(FateStayApplication.class, args);
    }
}
