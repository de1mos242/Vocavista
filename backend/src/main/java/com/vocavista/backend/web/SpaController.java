package com.vocavista.backend.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaController {

	@GetMapping({ "/", "/add", "/review", "/admin" })
	String frontendApp() {
		return "forward:/index.html";
	}

}
