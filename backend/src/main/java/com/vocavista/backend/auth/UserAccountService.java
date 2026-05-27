package com.vocavista.backend.auth;

import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserAccountService {

	private final UserAccountRepository userAccountRepository;
	private final Clock clock = Clock.systemUTC();

	@Transactional
	UserAccount upsertGoogleUser(String subject, String email, String displayName) {
		OffsetDateTime now = OffsetDateTime.now(clock);
		return userAccountRepository.findByProviderAndProviderSubject(AuthenticationProvider.GOOGLE, subject)
				.map(account -> update(account, email, displayName, now))
				.orElseGet(() -> userAccountRepository.save(UserAccount.google(subject, email, displayName, now)));
	}

	private UserAccount update(UserAccount account, String email, String displayName, OffsetDateTime now) {
		account.updateProfile(email, displayName, now);
		return userAccountRepository.save(account);
	}

}
