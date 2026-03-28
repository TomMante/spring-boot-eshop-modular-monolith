package com.github.eshopmodularmonolith.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;


@TestConfiguration
@Import({SecurityFilterChainConfig.class, WebConfig.class})
public class SecurityTestConfig {

    @Bean
    public JwtDecoder jwtDecoder(){
        return mock(JwtDecoder.class);
    }

}