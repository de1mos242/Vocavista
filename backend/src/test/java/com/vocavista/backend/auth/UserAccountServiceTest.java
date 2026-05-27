package com.vocavista.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

	@Mock
	private UserAccountRepository userAccountRepository;

	@InjectMocks
	private UserAccountService userAccountService;

	@Test
	void createsGoogleUserWhenSubjectIsNew() {
		when(userAccountRepository.findByProviderAndProviderSubject(AuthenticationProvider.GOOGLE, "google-sub"))
				.thenReturn(Optional.empty());
		when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserAccount account = userAccountService.upsertGoogleUser("google-sub", "learner@example.com", "German Learner");

		assertThat(account.getProvider()).isEqualTo(AuthenticationProvider.GOOGLE);
		assertThat(account.getProviderSubject()).isEqualTo("google-sub");
		assertThat(account.getEmail()).isEqualTo("learner@example.com");
		assertThat(account.getDisplayName()).isEqualTo("German Learner");
	}

	@Test
	void updatesGoogleUserOnRepeatedLogin() {
		UserAccount existing = UserAccount.google("google-sub", "old@example.com", "Old Name", OffsetDateTime.now().minusDays(1));
		when(userAccountRepository.findByProviderAndProviderSubject(AuthenticationProvider.GOOGLE, "google-sub"))
				.thenReturn(Optional.of(existing));
		when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userAccountService.upsertGoogleUser("google-sub", "learner@example.com", "German Learner");

		ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
		verify(userAccountRepository).save(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo("learner@example.com");
		assertThat(captor.getValue().getDisplayName()).isEqualTo("German Learner");
	}

}
