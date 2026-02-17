package com.github.eshopmodularmonolith.outboxjpa;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = OutboxJpaModuleConfig.BASE_PACKAGE)
public class OutboxJpaModuleConfig {

    public final static String BASE_PACKAGE = "com.github.eshopmodularmonolith.outboxjpa";

}
