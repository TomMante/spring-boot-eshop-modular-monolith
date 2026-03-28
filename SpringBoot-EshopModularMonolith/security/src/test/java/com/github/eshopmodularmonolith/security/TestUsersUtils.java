package com.github.eshopmodularmonolith.security;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public class TestUsersUtils {

    private TestUsersUtils() {
    }

    public static RequestPostProcessor user(String username){
        return jwt().jwt(jwt -> jwt.claim("preferred_username", username).build());
    }
}
