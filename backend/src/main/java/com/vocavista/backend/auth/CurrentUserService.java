package com.vocavista.backend.auth;

import com.vocavista.backend.api.model.CurrentUserResponse;
import com.vocavista.backend.api.model.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CurrentUserService {

	private final UserAccountRepository userAccountRepository;

	@Transactional(readOnly = true)
	CurrentUserResponse getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
			throw new AccessDeniedException("Current user is not an OIDC user");
		}

		UserAccount account = userAccountRepository
				.findByProviderAndProviderSubject(AuthenticationProvider.GOOGLE, oidcUser.getSubject())
				.orElseThrow(() -> new AccessDeniedException("Current user account was not found"));
		return new CurrentUserResponse(account.getId(), account.getEmail(), account.getDisplayName(), OAuthProvider.GOOGLE);
	}

}
