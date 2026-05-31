package com.vocavista.backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class FunctionalAccessWebConfig implements WebMvcConfigurer {

	private final FunctionalAccessInterceptor functionalAccessInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(functionalAccessInterceptor).addPathPatterns("/api/**");
	}

}
