package io.github.tommante.eshopmodularmonolith;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.context.event.EventListener;

//Exclude all configs as each module will define its own.
@SpringBootApplication(exclude = {
        DataMongoAutoConfiguration.class,
        DataMongoRepositoriesAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class
})
public class EshopModularMonolithApplication {

    private static final Logger log = LoggerFactory.getLogger(EshopModularMonolithApplication.class);

    private final String name;

    public EshopModularMonolithApplication(@Value("${spring.application.name} ") String name) {
        this.name = name;
    }

    static void main(String[] args) {
        SpringApplication.run(EshopModularMonolithApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("{} application started", name);
    }

}
