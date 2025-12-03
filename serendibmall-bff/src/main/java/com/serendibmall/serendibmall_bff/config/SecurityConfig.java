package com.serendibmall.serendibmall_bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/graphql", "/graphiql/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .formLogin(form -> {})
            .csrf(csrf -> csrf.disable()); // Disable CSRF for GraphQL API
        
        return http.build();
    }
}
