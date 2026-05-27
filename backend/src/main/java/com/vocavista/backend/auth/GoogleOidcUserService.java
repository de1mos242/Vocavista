package com.vocavista.backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private static final String GOOGLE_REGISTRATION_ID = "google";

	private final OidcUserService delegate = new OidcUserService();
	private final UserAccountService userAccountService;

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = delegate.loadUser(userRequest);
		if (GOOGLE_REGISTRATION_ID.equals(userRequest.getClientRegistration().getRegistrationId())) {
			userAccountService.upsertGoogleUser(subject(oidcUser), email(oidcUser), displayName(oidcUser));
		}
		return oidcUser;
	}

	private static String subject(OidcUser oidcUser) {
		String subject = oidcUser.getSubject();
		if (!StringUtils.hasText(subject)) {
			throw invalidUserInfo("Google user info did not include a subject");
		}
		return subject;
	}

	private static String email(OidcUser oidcUser) {
		String email = oidcUser.getClaimAsString(StandardClaimNames.EMAIL);
		if (!StringUtils.hasText(email)) {
			throw invalidUserInfo("Google user info did not include an email");
		}
		return email;
	}

	private static String displayName(OidcUser oidcUser) {
		String name = oidcUser.getClaimAsString(StandardClaimNames.NAME);
		return StringUtils.hasText(name) ? name : email(oidcUser);
	}

	private static OAuth2AuthenticationException invalidUserInfo(String description) {
		return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info", description, null), description);
	}

}
