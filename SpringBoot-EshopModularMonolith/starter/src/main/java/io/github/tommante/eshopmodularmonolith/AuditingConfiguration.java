package io.github.tommante.eshopmodularmonolith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Optional;

@EnableJpaAuditing
@EnableMongoAuditing
@Configuration
public class AuditingConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider(){
        return () -> Optional.of("system");
    }
}
