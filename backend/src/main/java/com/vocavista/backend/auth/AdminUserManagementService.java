package com.vocavista.backend.auth;

import com.vocavista.backend.api.model.AdminUserResponse;
import com.vocavista.backend.api.model.AdminUserStatusUpdateRequest;
import com.vocavista.backend.api.model.AdminUsersResponse;
import com.vocavista.backend.api.model.UserStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AdminUserManagementService {

	private final CurrentUserService currentUserService;
	private final UserAccountRepository userAccountRepository;
	private final UserAccessService userAccessService;

	@Transactional(readOnly = true)
	AdminUsersResponse listUsers() {
		UserAccount currentUser = currentUserService.getCurrentUserAccount();
		userAccessService.requireAdmin(currentUser);
		return new AdminUsersResponse(userAccountRepository.findAllByOrderByCreatedAtAsc().stream()
				.map(this::toResponse)
				.toList());
	}

	@Transactional
	AdminUserResponse updateStatus(UUID id, AdminUserStatusUpdateRequest request) {
		UserAccount currentUser = currentUserService.getCurrentUserAccount();
		userAccessService.requireAdmin(currentUser);
		if (request == null || request.getStatus() == null) {
			throw new AdminUserValidationException("status is required");
		}

		UserAccount target = userAccountRepository.findById(id)
				.orElseThrow(() -> new AdminUserNotFoundException("User account was not found"));
		if (userAccessService.isAdminListUser(target)) {
			throw new AccessDeniedException("Admin-list user statuses are protected");
		}

		target.setStatus(toAccountStatus(request.getStatus()));
		return toResponse(userAccountRepository.save(target));
	}

	private AdminUserResponse toResponse(UserAccount account) {
		return new AdminUserResponse(account.getId(), account.getEmail(), account.getDisplayName(),
				toApiStatus(account.getStatus()), userAccessService.isAdminListUser(account));
	}

	private static UserStatus toApiStatus(UserAccountStatus status) {
		return UserStatus.fromValue(status.value());
	}

	private static UserAccountStatus toAccountStatus(UserStatus status) {
		return UserAccountStatus.fromValue(status.getValue());
	}

}
