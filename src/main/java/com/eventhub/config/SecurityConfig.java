package com.eventhub.config;


import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.eventhub.config.RestApis.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/" + AUTH + LOG_IN, "/" + AUTH + REFRESH, "/" + AUTH + LOG_OUT).permitAll()
                        .requestMatchers(HttpMethod.POST, "/" + AUTH + SIGN_IN).permitAll()
                        .requestMatchers(HttpMethod.PUT, "/" + AUTH + "/update").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/" + AUTH + "/change-password").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/" + AUTH + DELETE_USER).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/" + AUTH + GET_USERS).hasRole("ADMIN")

                        // Sertifika doğrulama: herkese açık (QR ile tarayan herkes doğrulayabilmeli)
                        .requestMatchers(HttpMethod.GET, "/" + VERIFY + "/**").permitAll()

                        // Etkinlik keşif/detay: herkese açık; oluşturma/düzenleme ise kimlik doğrulaması gerektirir
                        .requestMatchers(HttpMethod.GET, "/" + EVENT + "/**").permitAll()
                        .requestMatchers("/" + EVENT + "/**").authenticated()

                        // Admin onay kuyruğu: yalnızca ADMIN
                        .requestMatchers("/" + ADMIN_EVENT + "/**").hasRole("ADMIN")

                        // Kurumlar/kategoriler: herkes görüntüleyebilir, CRUD ADMIN'e özel
                        .requestMatchers(HttpMethod.GET, "/" + INSTITUTION + "/**").permitAll()
                        .requestMatchers("/" + INSTITUTION + "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/" + CATEGORY + "/**").permitAll()
                        .requestMatchers("/" + CATEGORY + "/**").hasRole("ADMIN")

                        .requestMatchers("/" + REGISTRATION + "/**").authenticated()
                        .requestMatchers("/" + CHECKIN + "/**").authenticated()
                        .requestMatchers("/" + CERTIFICATE + "/**").authenticated()

                        .requestMatchers("/" + AUDIT + "/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Oturum doğrulanamadı.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Bu işlem için yetkiniz yok.\"}");
                        })
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .xssProtection(HeadersConfigurer.XXssConfig::disable)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src * 'unsafe-inline' 'unsafe-eval'; " +
                                        "script-src * 'unsafe-inline' 'unsafe-eval'; " +
                                        "style-src * 'unsafe-inline';")
                        )
                )
                .authenticationProvider(authenticationProvider)
                .addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
