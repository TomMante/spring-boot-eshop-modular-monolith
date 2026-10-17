package io.github.tommante.eshopmodularmonolith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }
}
