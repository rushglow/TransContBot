package org.batukhtin.transcontbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransContBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransContBotApplication.class, args);
    }

}
