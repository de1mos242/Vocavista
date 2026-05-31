package com.vocavista.backend.auth;

import java.util.Arrays;

public enum UserAccountStatus {

	PENDING("pending"),
	ACTIVE("active"),
	DEACTIVATED("deactivated");

	private final String value;

	UserAccountStatus(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	static UserAccountStatus fromValue(String value) {
		return Arrays.stream(values())
				.filter(status -> status.value.equals(value))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown user account status: " + value));
	}

}
