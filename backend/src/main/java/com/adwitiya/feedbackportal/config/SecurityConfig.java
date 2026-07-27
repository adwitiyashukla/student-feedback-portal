package com.adwitiya.feedbackportal.config;

import com.adwitiya.feedbackportal.config.properties.AppProperties;
import com.adwitiya.feedbackportal.security.AppUserDetailsService;
import com.adwitiya.feedbackportal.security.JwtAuthenticationFilter;
import com.adwitiya.feedbackportal.security.RateLimitFilter;
import com.adwitiya.feedbackportal.security.RestAccessDeniedHandler;
import com.adwitiya.feedbackportal.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Two independent security filter chains for two independent clients.
 *
 * <p>The REST API is stateless and authenticates with a bearer token; CSRF
 * protection is not applicable there because the browser never attaches
 * credentials automatically. The server-rendered UI uses a session cookie, so
 * it keeps CSRF tokens and form login. Expressing this as two ordered
 * {@link SecurityFilterChain} beans is cleaner and safer than trying to make
 * one chain serve both.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Endpoints reachable without authentication, on either chain. */
    private static final String[] PUBLIC_API_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/health/**"
    };

    private static final String[] PUBLIC_WEB_PATHS = {
            "/", "/login", "/error",
            "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico",
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final AppUserDetailsService userDetailsService;
    private final AppProperties appProperties;

    /**
     * Chain 1 — the JSON API. Stateless, bearer-token authenticated, rate
     * limited, and it answers with problem+json rather than a redirect.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/dashboard/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .headers(this::applySecurityHeaders)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Chain 2 — the Thymeleaf UI. Session cookie, CSRF tokens, form login.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_WEB_PATHS).permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/settings/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")          // POST only; never GET
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(new RoleAwareLoginSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?loggedOut")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false))
                .exceptionHandling(handling -> handling
                        .accessDeniedPage("/error/403"))
                .headers(this::applySecurityHeaders);

        return http.build();
    }

    /**
     * Response headers applied to both chains.
     *
     * <p>The CSP is intentionally strict; the UI ships its own CSS and JS and
     * loads Chart.js from a pinned CDN entry.</p>
     */
    private void applySecurityHeaders(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<HttpSecurity> headers) {
        headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31_536_000))
                .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                        "default-src 'self'",
                        "script-src 'self' https://cdn.jsdelivr.net",
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net",
                        "img-src 'self' data:",
                        "font-src 'self' https://cdn.jsdelivr.net",
                        "connect-src 'self'",
                        "frame-ancestors 'none'",
                        "base-uri 'self'",
                        "form-action 'self'")));
    }

    /**
     * BCrypt at strength 12, wrapped in a {@link DelegatingPasswordEncoder}.
     *
     * <p>New hashes are written with a {@code {bcrypt}} prefix, which is what
     * makes a future algorithm change a configuration edit rather than a
     * forced password reset for every account. Hashes stored without a prefix
     * — the demo dataset, and anything imported from an older system — are
     * still verified, via {@code setDefaultPasswordEncoderForMatches}.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        String encodingId = "bcrypt";
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

        DelegatingPasswordEncoder encoder =
                new DelegatingPasswordEncoder(encodingId, Map.of(encodingId, bcrypt));
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Distinguish "no such user" from "wrong password" internally only.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /** Sends each role to its own landing page after a successful form login. */
    static class RoleAwareLoginSuccessHandler
            extends org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler {

        RoleAwareLoginSuccessHandler() {
            setAlwaysUseDefaultTargetUrl(false);
            setTargetUrlParameter("continue");
        }

        @Override
        protected String determineTargetUrl(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            org.springframework.security.core.Authentication authentication) {
            boolean staff = authentication.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_SUPER_ADMIN"));
            return staff ? "/admin/dashboard" : "/student/dashboard";
        }
    }

    /** Convenience matcher used by tests. */
    public static AntPathRequestMatcher apiMatcher(String pattern) {
        return AntPathRequestMatcher.antMatcher(pattern);
    }
}
