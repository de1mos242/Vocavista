package com.vocavista.backend.auth;

import org.springframework.util.StringUtils;

final class OAuthLoginRedirects {

	static final String SESSION_ATTRIBUTE = "vocavista.oauth2.redirect";

	private OAuthLoginRedirects() {
	}

	static String sanitize(String redirect) {
		if (StringUtils.hasText(redirect) && redirect.startsWith("/") && !redirect.startsWith("//")) {
			return redirect;
		}
		return "/";
	}

}
