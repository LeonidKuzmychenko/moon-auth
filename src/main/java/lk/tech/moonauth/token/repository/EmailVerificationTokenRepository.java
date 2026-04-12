package lk.tech.moonauth.token.repository;

import lk.tech.moonauth.token.entity.EmailVerificationToken;
import lk.tech.moonauth.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    List<EmailVerificationToken> findAllByUserAndUsedFalse(User user);
}
