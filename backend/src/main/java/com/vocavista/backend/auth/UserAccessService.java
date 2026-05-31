package com.vocavista.backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccessService {

	private final AdminProperties adminProperties;

	public boolean isAdminListUser(UserAccount account) {
		return account != null && adminProperties.isAdminEmail(account.getEmail());
	}

	public boolean canUseFunctionalFeatures(UserAccount account) {
		return isAdminListUser(account) || account.getStatus() == UserAccountStatus.ACTIVE;
	}

	public void requireAdmin(UserAccount account) {
		if (!isAdminListUser(account)) {
			throw new AccessDeniedException("Admin access is required");
		}
	}

	public void requireFunctionalAccess(UserAccount account) {
		if (!canUseFunctionalFeatures(account)) {
			throw new AccessDeniedException("Account is not approved to use app features");
		}
	}

}
