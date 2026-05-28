package com.vocavista.backend.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocavista.backend.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@AfterEach
	void tearDown() {
		userAccountRepository.deleteAll();
	}

	@Test
	void returnsCurrentUser() throws Exception {
		UserAccount account = userAccountRepository.save(UserAccount.google("google-sub", "learner@example.com", "German Learner",
				OffsetDateTime.now()));

		mockMvc.perform(get("/api/v1/auth/me").with(oidcLogin().idToken(token -> token.subject("google-sub"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(account.getId().toString()))
				.andExpect(jsonPath("$.email").value("learner@example.com"))
				.andExpect(jsonPath("$.displayName").value("German Learner"))
				.andExpect(jsonPath("$.provider").value("google"));
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
    void securesExistingApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/words/suggestions").param("query", "haus"))
                .andExpect(status().isUnauthorized());
    }

	@Test
	void logsOutWithNoContentResponse() throws Exception {
		mockMvc.perform(post("/logout").with(oidcLogin()))
				.andExpect(status().isNoContent());
	}

	@Test
	void startsGoogleLoginWithReturnPage() throws Exception {
		mockMvc.perform(get("/login/google").param("redirect", "/veo-video.html"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/oauth2/authorization/google"))
				.andExpect(request().sessionAttribute(OAuthLoginRedirects.SESSION_ATTRIBUTE, "/veo-video.html"));
	}

	@Test
	void servesReviewPageWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/review.html"))
				.andExpect(status().isOk());
	}

	@Test
	void servesHomePageWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsExternalLoginReturnUrl() throws Exception {
		mockMvc.perform(get("/login/google").param("redirect", "//evil.example"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/oauth2/authorization/google"))
				.andExpect(request().sessionAttribute(OAuthLoginRedirects.SESSION_ATTRIBUTE, "/"));
	}

}
