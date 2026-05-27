package com.vocavista.backend.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final GoogleOidcUserService googleOidcUserService;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/error").permitAll()
				.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
				.requestMatchers("/veo-video.html", "/").permitAll()
				.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout"))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
					if (request.getRequestURI().startsWith("/api/")) {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					}
					else {
						response.sendRedirect("/oauth2/authorization/google");
					}
				}))
                .oauth2Login(oauth -> oauth.userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService)))
                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
        return http.build();
    }

}
