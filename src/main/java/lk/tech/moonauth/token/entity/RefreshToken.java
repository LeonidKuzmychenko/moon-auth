package lk.tech.moonauth.token.entity;

import jakarta.persistence.*;
import lk.tech.moonauth.user.entity.User;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    private LocalDateTime revokedAt;

    private String replacedByTokenHash;

    private String userAgent;

    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
