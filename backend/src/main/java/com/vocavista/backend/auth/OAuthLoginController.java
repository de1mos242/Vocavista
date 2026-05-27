package com.vocavista.backend.auth;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
class OAuthLoginController {

	@GetMapping("/login/google")
	String startGoogleLogin(@RequestParam(required = false) String redirect, HttpSession session) {
		session.setAttribute(OAuthLoginRedirects.SESSION_ATTRIBUTE, OAuthLoginRedirects.sanitize(redirect));
		return "redirect:/oauth2/authorization/google";
	}

}
