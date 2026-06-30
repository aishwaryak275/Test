package com.teleconnect.analytics_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/reports/arpu/**").hasAnyRole("ADMIN", "BILLING")
                .requestMatchers("/api/reports/churn/**").hasAnyRole("ADMIN", "COMPLIANCE")
                .requestMatchers("/api/reports/network-utilisation/**").hasAnyRole("ADMIN", "NETWORK_OPS")
                .requestMatchers("/api/reports/sla-compliance/**").hasAnyRole("ADMIN", "NETWORK_OPS", "COMPLIANCE")
                .requestMatchers("/api/reports/collection-efficiency/**").hasAnyRole("ADMIN", "BILLING")
                .requestMatchers("/api/reports/subscriber-growth/**").hasAnyRole("ADMIN", "COMPLIANCE")
                .requestMatchers("/api/reports/generate").hasAnyRole("ADMIN", "BILLING", "NETWORK_OPS")
                .requestMatchers("/api/reports/*/export").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(org.springframework.security.config.Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN", "BILLING", "NETWORK_OPS", "COMPLIANCE")
                .build();

        UserDetails billing = User.builder()
                .username("billing")
                .password(encoder.encode("billing123"))
                .roles("BILLING")
                .build();

        UserDetails networkOps = User.builder()
                .username("networkops")
                .password(encoder.encode("network123"))
                .roles("NETWORK_OPS")
                .build();

        UserDetails compliance = User.builder()
                .username("compliance")
                .password(encoder.encode("compliance123"))
                .roles("COMPLIANCE")
                .build();

        return new InMemoryUserDetailsManager(admin, billing, networkOps, compliance);
    }
}
