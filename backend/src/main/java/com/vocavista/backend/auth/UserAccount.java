package com.vocavista.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthenticationProvider provider;

	@Column(name = "provider_subject", nullable = false)
	private String providerSubject;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private UserAccountStatus status;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "last_login_at", nullable = false)
	private OffsetDateTime lastLoginAt;

	static UserAccount google(String subject, String email, String displayName, UserAccountStatus status, OffsetDateTime now) {
		UserAccount account = new UserAccount();
		account.id = UUID.randomUUID();
		account.provider = AuthenticationProvider.GOOGLE;
		account.providerSubject = subject;
		account.email = email;
		account.status = status;
		account.displayName = displayName;
		account.createdAt = now;
		account.updatedAt = now;
		account.lastLoginAt = now;
		return account;
	}

	void updateProfile(String email, String displayName, OffsetDateTime now) {
		this.email = email;
		this.displayName = displayName;
		this.updatedAt = now;
		this.lastLoginAt = now;
	}

}
