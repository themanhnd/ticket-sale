package com.ticketsale.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
//    Gateway dùng WebFlux, vì Spring Cloud Gateway chạy trên reactive stack.
//    Do đó security config dùng: SecurityWebFilterChain và ServerHttpSecurity
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) //Với API gateway stateless, thường tắt CSRF.
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyExchange().permitAll()) //Phase 1 chưa làm JWT thật, nên tạm permit all.
                .build();
    }
}