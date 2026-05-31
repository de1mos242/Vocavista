package com.vocavista.backend.auth;

import com.vocavista.backend.api.AdminApi;
import com.vocavista.backend.api.model.AdminUserResponse;
import com.vocavista.backend.api.model.AdminUserStatusUpdateRequest;
import com.vocavista.backend.api.model.AdminUsersResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequireFunctionalAccess
class AdminController implements AdminApi {

	private final AdminUserManagementService adminUserManagementService;

	@Override
	public ResponseEntity<AdminUsersResponse> listAdminUsers() {
		return ResponseEntity.ok(adminUserManagementService.listUsers());
	}

	@Override
	public ResponseEntity<AdminUserResponse> updateAdminUserStatus(UUID id,
			AdminUserStatusUpdateRequest adminUserStatusUpdateRequest) {
		return ResponseEntity.ok(adminUserManagementService.updateStatus(id, adminUserStatusUpdateRequest));
	}

}
