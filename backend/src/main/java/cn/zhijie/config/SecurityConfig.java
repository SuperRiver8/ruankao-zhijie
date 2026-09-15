package cn.zhijie.config;

import cn.zhijie.security.*;
import cn.zhijie.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        AuthService auth,
        SecurityErrorHandler errors,
        org.springframework.core.env.Environment environment,
        @Value("${app.cors-origin}") String origins
    ) throws Exception {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.asList(origins.split(",")));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return http
            .csrf(x -> x.disable())
            .cors(x -> x.configurationSource(source))
            .sessionManagement(x -> x.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(x -> {
                if (environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false)) x
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll();
                x.requestMatchers("/actuator/health").permitAll();
                x
                    .requestMatchers(
                        "/api/customer/auth/register",
                        "/api/customer/auth/login",
                        "/api/customer/auth/refresh",
                        "/api/admin/auth/login",
                        "/api/admin/auth/refresh",
                        "/api/public/**"
                    )
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasAuthority("IDENTITY_ADMIN")
                    .requestMatchers(
                        "/api/customer/**",
                        "/api/exams/**",
                        "/api/practice",
                        "/api/learning/**",
                        "/api/me/targets/**"
                    )
                    .hasAuthority("IDENTITY_CUSTOMER")
                    .anyRequest()
                    .authenticated();
            })
            .exceptionHandling(x -> x.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .addFilterBefore(
                new JwtAuthenticationFilter(auth, errors),
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}
