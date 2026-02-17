package com.github.eshopmodularmonolith;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class EshopModularMonolithStarter {

    private static final Logger log = LoggerFactory.getLogger(EshopModularMonolithStarter.class);

    private final String name;

    public EshopModularMonolithStarter(@Value("${spring.application.name} ") String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        SpringApplication.run(EshopModularMonolithStarter.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("{} application started", name);
    }


}
