package com.vocavista.backend.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByProviderAndProviderSubject(AuthenticationProvider provider, String providerSubject);

	List<UserAccount> findAllByOrderByCreatedAtAsc();

}
