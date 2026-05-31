package com.vocavista.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class FunctionalAccessInterceptor implements HandlerInterceptor {

	private final CurrentUserService currentUserService;
	private final UserAccessService userAccessService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod handlerMethod && requiresFunctionalAccess(handlerMethod)) {
			userAccessService.requireFunctionalAccess(currentUserService.getCurrentUserAccount());
		}
		return true;
	}

	private static boolean requiresFunctionalAccess(HandlerMethod handlerMethod) {
		return handlerMethod.hasMethodAnnotation(RequireFunctionalAccess.class)
				|| handlerMethod.getBeanType().isAnnotationPresent(RequireFunctionalAccess.class);
	}

}
