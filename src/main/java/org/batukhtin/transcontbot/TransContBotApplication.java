package org.batukhtin.transcontbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableScheduling
public class TransContBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransContBotApplication.class, args);
    }

}
