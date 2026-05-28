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
public class CurrentUserService {

	private final UserAccountRepository userAccountRepository;

	@Transactional(readOnly = true)
	public CurrentUserResponse getCurrentUser() {
		UserAccount account = getCurrentUserAccount();
		return new CurrentUserResponse(account.getId(), account.getEmail(), account.getDisplayName(), OAuthProvider.GOOGLE);
	}

	@Transactional(readOnly = true)
	public UserAccount getCurrentUserAccount() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
			throw new AccessDeniedException("Current user is not an OIDC user");
		}

		return userAccountRepository
				.findByProviderAndProviderSubject(AuthenticationProvider.GOOGLE, oidcUser.getSubject())
				.orElseThrow(() -> new AccessDeniedException("Current user account was not found"));
	}

}
