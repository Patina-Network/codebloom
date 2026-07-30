package org.patinanetwork.codebloom.api.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * This is where the OAuth provider lives.
 *
 * @see <a href= "https://github.com/tahminator/codebloom/tree/main/docs/auth.md">Authentication Documentation</a>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfig(final AuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Bean
    @Order(2)
    SecurityFilterChain embedSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher("/embed/potd", "/embed/leaderboard")
                .headers(headers -> headers.frameOptions(f -> f.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors *")))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * The authorization endpoint is used to get redirected to the OAuth login page. The redirection endpoint is the
     * callback endpoint on our server that then handles the authentication logic. This handles all other requests with
     * OAuth.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain oauthSecurityFilterChain(final HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2.authorizationEndpoint(auth -> auth
                                // This baseUri implicitly has {registrationId} at the end of it. That's why
                                // /api/auth/flow/discord still works.
                                .baseUri("/api/auth/flow"))
                        .redirectionEndpoint(auth -> auth.baseUri("/api/auth/flow/callback/{registrationId}"))
                        .successHandler(customAuthenticationSuccessHandler));

        return http.build();
    }

    // Remove the default login form that comes with SpringBoot Security.
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/login");
    }
}
