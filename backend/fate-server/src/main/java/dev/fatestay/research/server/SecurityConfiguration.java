package dev.fatestay.research.server;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/bazi/charts", "/api/v1/bazi/flow-months", "/api/v1/bazi/flow-days"))
                .authorizeHttpRequests(requests -> requests
                .requestMatchers(HttpMethod.POST, "/api/v1/bazi/charts", "/api/v1/bazi/flow-months", "/api/v1/bazi/flow-days").permitAll()
                .requestMatchers("/api/v1/health", "/actuator/health").permitAll()
                .anyRequest().denyAll())
                .build();
    }
}
