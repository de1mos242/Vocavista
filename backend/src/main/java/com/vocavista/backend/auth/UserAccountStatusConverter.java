package com.vocavista.backend.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class UserAccountStatusConverter implements AttributeConverter<UserAccountStatus, String> {

	@Override
	public String convertToDatabaseColumn(UserAccountStatus status) {
		return status == null ? null : status.value();
	}

	@Override
	public UserAccountStatus convertToEntityAttribute(String value) {
		return value == null ? null : UserAccountStatus.fromValue(value);
	}

}
