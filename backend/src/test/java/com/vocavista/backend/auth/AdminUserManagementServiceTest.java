package com.vocavista.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.AdminUserStatusUpdateRequest;
import com.vocavista.backend.api.model.AdminUsersResponse;
import com.vocavista.backend.api.model.UserStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

	@Mock
	private CurrentUserService currentUserService;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private UserAccessService userAccessService;

	@InjectMocks
	private AdminUserManagementService adminUserManagementService;

	@Test
	void listsUsersWithAdminProtectionState() {
		UserAccount admin = account("admin-sub", "admin@example.com", UserAccountStatus.ACTIVE);
		UserAccount learner = account("learner-sub", "learner@example.com", UserAccountStatus.PENDING);
		when(currentUserService.getCurrentUserAccount()).thenReturn(admin);
		when(userAccountRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(admin, learner));
		when(userAccessService.isAdminListUser(admin)).thenReturn(true);
		when(userAccessService.isAdminListUser(learner)).thenReturn(false);

		AdminUsersResponse response = adminUserManagementService.listUsers();

		verify(userAccessService).requireAdmin(admin);
		assertThat(response.getItems()).hasSize(2);
		assertThat(response.getItems().getFirst().getAdminListUser()).isTrue();
		assertThat(response.getItems().get(1).getStatus()).isEqualTo(UserStatus.PENDING);
	}

	@Test
	void updatesNonAdminUserStatus() {
		UserAccount admin = account("admin-sub", "admin@example.com", UserAccountStatus.ACTIVE);
		UserAccount learner = account("learner-sub", "learner@example.com", UserAccountStatus.PENDING);
		when(currentUserService.getCurrentUserAccount()).thenReturn(admin);
		when(userAccountRepository.findById(learner.getId())).thenReturn(Optional.of(learner));
		when(userAccessService.isAdminListUser(learner)).thenReturn(false);
		when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

		adminUserManagementService.updateStatus(learner.getId(), new AdminUserStatusUpdateRequest(UserStatus.ACTIVE));

		assertThat(learner.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
		verify(userAccountRepository).save(learner);
	}

	@Test
	void rejectsStatusUpdatesForAdminListUsers() {
		UserAccount admin = account("admin-sub", "admin@example.com", UserAccountStatus.ACTIVE);
		UserAccount protectedUser = account("protected-sub", "protected@example.com", UserAccountStatus.ACTIVE);
		when(currentUserService.getCurrentUserAccount()).thenReturn(admin);
		when(userAccountRepository.findById(protectedUser.getId())).thenReturn(Optional.of(protectedUser));
		when(userAccessService.isAdminListUser(protectedUser)).thenReturn(true);

		assertThatThrownBy(() -> adminUserManagementService.updateStatus(protectedUser.getId(),
				new AdminUserStatusUpdateRequest(UserStatus.DEACTIVATED)))
				.isInstanceOf(AccessDeniedException.class);
		verify(userAccountRepository, never()).save(any());
	}

	private static UserAccount account(String subject, String email, UserAccountStatus status) {
		return UserAccount.google(subject, email, email, status, OffsetDateTime.now());
	}

}
