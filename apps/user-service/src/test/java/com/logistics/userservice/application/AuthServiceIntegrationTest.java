package com.logistics.userservice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.port.HubClientPort;
import com.logistics.userservice.application.token.TokenClaims;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.config.test.ConcurrencyTestingUtil;
import com.logistics.userservice.domain.RequestedRole;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.domain.redis.SessionRepository;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.infrastructure.security.JwtTokenProvider;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@DisplayName("AuthService 통합 테스트")
class AuthServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private RoleCacheRepository roleCacheRepository;
    @Autowired private AuthService authService;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private StringRedisTemplate redisTemplate;

    @MockitoBean private HubClientPort hubClientPort;

    private User companyManagerUser;
    private User deliveryUser;

    @BeforeEach
    void setUp() {
        UserCreateCommand companyManagerCommand =
                new UserCreateCommand(
                        "company1234",
                        passwordEncoder.encode("Testtest123!"),
                        "김철수",
                        "U123456789",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        RequestedRole.COMPANY_MANAGER);
        companyManagerUser = User.create(companyManagerCommand);
        companyManagerUser.approve(UUID.randomUUID());
        companyManagerUser = userRepository.saveAndFlush(companyManagerUser);

        UserCreateCommand deliveryCommand =
                new UserCreateCommand(
                        "delivery1234",
                        passwordEncoder.encode("Testtest123!"),
                        "김철수",
                        "U123354789",
                        UUID.randomUUID(),
                        null,
                        RequestedRole.HUB_DELIVERY);
        deliveryUser = User.create(deliveryCommand);
        deliveryUser.approve(UUID.randomUUID());
        deliveryUser.completeProvisioning();
        deliveryUser = userRepository.saveAndFlush(deliveryUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Nested
    @DisplayName("로그인 테스트")
    class Login {
        @Test
        @DisplayName("로그인에 성공하면 액세스 토큰과 리프레시 토큰을 발급한다.")
        void login_success() {
            // given
            String username = companyManagerUser.getUsername();

            LoginRequest loginRequest = new LoginRequest(username, "Testtest123!");

            // when
            TokenResult tokens = authService.login(loginRequest.toCommand());

            // then
            assertThat(tokens.accessToken()).isNotBlank();
            assertThat(tokens.refreshToken()).isNotBlank();
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("배송 담당자는 최대 세션 5개 다른 역할은 세션 1개를 유지한다.")
        void login_success_with_roleSessionLimit() throws InterruptedException {
            // given
            UUID companyUserId = companyManagerUser.getId();
            UUID deliveryUserId = deliveryUser.getId();

            String companyUsername = companyManagerUser.getUsername();
            String deliveryUsername = deliveryUser.getUsername();

            LoginRequest companyLoginRequest = new LoginRequest(companyUsername, "Testtest123!");
            LoginRequest deliveryLoginRequest = new LoginRequest(deliveryUsername, "Testtest123!");

            int threadCount = 6;

            // when
            ConcurrencyTestingUtil.run(
                    threadCount, () -> authService.login(companyLoginRequest.toCommand()));

            ConcurrencyTestingUtil.run(
                    threadCount, () -> authService.login(deliveryLoginRequest.toCommand()));

            Long companySessionCount =
                    redisTemplate.opsForZSet().size("user:sessions:" + companyUserId);
            Long deliverySessionCount =
                    redisTemplate.opsForZSet().size("user:sessions:" + deliveryUserId);

            // then
            assertThat(companySessionCount).isEqualTo(1L);
            assertThat(deliverySessionCount).isEqualTo(5L);
        }
    }

    @Test
    @DisplayName("로그아웃에 성공하면 Redis에서 인증정보를 삭제한다.")
    void logout_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        TokenClaims tokenClaims = new TokenClaims(userId, UUID.randomUUID(), UUID.randomUUID());

        String accessToken = jwtTokenProvider.generateAccessToken(tokenClaims, sessionId);
        String refreshToken = "refreshToken";

        sessionRepository.save(userId, sessionId, 1);
        refreshTokenRepository.save(userId, sessionId, refreshToken);
        roleCacheRepository.save(userId, "master");

        // when
        authService.logout(accessToken);
        Set<String> keys = redisTemplate.keys("*");

        // then
        assertThat(keys).isEmpty();
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class reissue {
        @Test
        @DisplayName("토큰 재발급에 성공한다.")
        void reissue_success() {
            // given
            UUID userId = companyManagerUser.getId();
            UUID sessionId = UUID.randomUUID();

            TokenClaims tokenClaims = new TokenClaims(userId, UUID.randomUUID(), UUID.randomUUID());

            String refreshToken = jwtTokenProvider.generateRefreshToken(tokenClaims, sessionId);
            refreshTokenRepository.save(userId, sessionId, refreshToken);
            sessionRepository.save(userId, sessionId, 1);

            // when
            TokenResult tokenResult = authService.reissue(refreshToken);

            // then
            assertThat(refreshToken)
                    .isNotEqualTo(refreshTokenRepository.findByUserId(userId, sessionId).get());
            assertThat(tokenResult.refreshToken())
                    .isEqualTo(refreshTokenRepository.findByUserId(userId, sessionId).get());
            assertThat(tokenResult.refreshToken()).isNotEqualTo(refreshToken);
            assertThat(sessionRepository.exists(userId, sessionId)).isTrue();
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("토큰 재발급을 동시에 요청해도 하나만 성공한다.")
        void reissue_only_one_success() throws InterruptedException {
            // given
            UUID userId = deliveryUser.getId();
            UUID sessionId = UUID.randomUUID();

            TokenClaims tokenClaims = new TokenClaims(userId, UUID.randomUUID(), UUID.randomUUID());

            String refreshToken = jwtTokenProvider.generateRefreshToken(tokenClaims, sessionId);
            refreshTokenRepository.save(userId, sessionId, refreshToken);
            sessionRepository.save(userId, sessionId, 5);

            int threadCount = 5;

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // when
            ConcurrencyTestingUtil.run(
                    threadCount,
                    () -> {
                        try {
                            authService.reissue(refreshToken);
                            successCount.incrementAndGet();
                        } catch (BusinessException e) {
                            if (e.getErrorCode() == AuthErrorCode.TOKEN_INVALID)
                                failureCount.incrementAndGet();
                        }
                    });

            Set<String> keys = redisTemplate.keys("user:refresh:" + "{" + userId + "}:" + "*");

            // then
            AssertionsForClassTypes.assertThat(successCount.get()).isEqualTo(1);
            AssertionsForClassTypes.assertThat(failureCount.get()).isEqualTo(4);
            assertThat(keys.size()).isEqualTo(1);
            assertThat(refreshTokenRepository.findByUserId(userId, sessionId).get())
                    .isNotEqualTo(refreshToken);
        }
    }
}
