package com.vocavista.backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AdminUserBackfill {

	private final UserAccountRepository userAccountRepository;
	private final UserAccessService userAccessService;

	@Transactional
	@EventListener(ApplicationReadyEvent.class)
	void activateConfiguredAdmins() {
		for (UserAccount account : userAccountRepository.findAll()) {
			if (userAccessService.isAdminListUser(account) && account.getStatus() != UserAccountStatus.ACTIVE) {
				account.setStatus(UserAccountStatus.ACTIVE);
				userAccountRepository.save(account);
			}
		}
	}

}
