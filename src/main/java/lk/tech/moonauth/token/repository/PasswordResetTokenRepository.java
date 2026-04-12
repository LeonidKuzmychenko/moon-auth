package lk.tech.moonauth.token.repository;

import lk.tech.moonauth.token.entity.PasswordResetToken;
import lk.tech.moonauth.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    List<PasswordResetToken> findAllByUserAndUsedFalse(User user);
}
