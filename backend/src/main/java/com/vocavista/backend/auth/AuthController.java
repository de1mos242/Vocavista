package com.vocavista.backend.auth;

import com.vocavista.backend.api.AuthApi;
import com.vocavista.backend.api.model.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController implements AuthApi {

	private final CurrentUserService currentUserService;

	@Override
	public ResponseEntity<CurrentUserResponse> getCurrentUser() {
		return ResponseEntity.ok(currentUserService.getCurrentUser());
	}

}
