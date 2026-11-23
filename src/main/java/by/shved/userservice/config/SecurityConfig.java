package by.shved.userservice.config;

import by.shved.userservice.security.filter.InternalAuthenticationFilter;
import by.shved.userservice.security.filter.JwtAuthenticationFilter;
import by.shved.userservice.security.RestSecurityExceptionHandler;
import by.shved.userservice.security.filter.SingleAuthenticationMechanismFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final RestSecurityExceptionHandler securityExceptionHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalAuthenticationFilter internalAuthenticationFilter;
    private final SingleAuthenticationMechanismFilter singleAuthenticationMechanismFilter;

    @Bean
    public FilterRegistrationBean<SingleAuthenticationMechanismFilter> singleFilterRegistration(
            SingleAuthenticationMechanismFilter singleAuthenticationMechanismFilter) {
        FilterRegistrationBean<SingleAuthenticationMechanismFilter> registration
                = new FilterRegistrationBean<>(singleAuthenticationMechanismFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<InternalAuthenticationFilter> internalFilterRegistration(
            InternalAuthenticationFilter internalAuthenticationFilter) {
        FilterRegistrationBean<InternalAuthenticationFilter> registration =
                new FilterRegistrationBean<>(internalAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalAuthenticationFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(singleAuthenticationMechanismFilter, InternalAuthenticationFilter.class);
        return http.build();
    }
}
