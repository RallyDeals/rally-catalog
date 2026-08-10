package com.rally.catalog.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // ====================================================================
    // ADMIN role filter — DISABLED until the Auth service is implemented.
    // The catalog service does its own role filtering via AdminRoleFilter;
    // enable it by uncommenting this bean (or the @Component on the class).
    // ====================================================================
    // @Bean
    // public FilterRegistrationBean<AdminRoleFilter> adminRoleFilter() {
    //     FilterRegistrationBean<AdminRoleFilter> bean = new FilterRegistrationBean<>(new AdminRoleFilter());
    //     bean.addUrlPatterns("/products/admin/*");
    //     return bean;
    // }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
