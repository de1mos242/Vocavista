package com.vocavista.backend.auth;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "vocavista.admin")
@Getter
@Setter
public class AdminProperties {

	private List<String> emails = List.of();

	public boolean isAdminEmail(String email) {
		return normalizedEmails().contains(normalize(email));
	}

	Set<String> normalizedEmails() {
		Set<String> normalized = new LinkedHashSet<>();
		for (String email : emails) {
			String value = normalize(email);
			if (StringUtils.hasText(value)) {
				normalized.add(value);
			}
		}
		return normalized;
	}

	private static String normalize(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

}
