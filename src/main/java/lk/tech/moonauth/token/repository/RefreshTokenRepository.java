package lk.tech.moonauth.token.repository;

import lk.tech.moonauth.token.entity.RefreshToken;
import lk.tech.moonauth.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findAllByUserAndRevokedFalse(User user);
    void deleteAllByUser(User user);
}
