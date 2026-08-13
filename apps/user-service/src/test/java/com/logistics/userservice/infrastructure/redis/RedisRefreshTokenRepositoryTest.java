package com.logistics.userservice.infrastructure.redis;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.config.test.ConcurrencyTestingUtil;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.error.UserErrorCode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@DisplayName("RedisRefreshTokenRepositoryTest - 통합 테스트")
class RedisRefreshTokenRepositoryTest extends AbstractIntegrationTest {
    @Autowired private RedisRefreshTokenRepository refreshTokenRepository;
    @Autowired private StringRedisTemplate redisTemplate;


    @BeforeEach
    void setUp() {
        redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("리프레시 토큰을 저장한다.")
    void save_refresh_token() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String refreshToken = "refreshToken";

        // when
        refreshTokenRepository.save(userId, sessionId, refreshToken);

        // then
        assertThat(refreshTokenRepository.findByUserId(userId, sessionId))
                .contains(refreshToken);
    }

    @Nested
    @DisplayName("리프레시 토큰 재발급")
    class Rotate {
        @Test
        @DisplayName("토큰 재발급 시 기존 리프레시 토큰과 비교 후 일치하면 재발급한다.")
        void reissue_success() {
            // given
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String refreshToken = "refreshToken";
            String newRefreshToken = "newRefreshToken";

            refreshTokenRepository.save(userId, sessionId, refreshToken);

            // when
            boolean result =
                    refreshTokenRepository.rotate(userId, sessionId, refreshToken, newRefreshToken);

            // then
            assertThat(result).isTrue();
            assertThat(refreshTokenRepository.findByUserId(userId, sessionId))
                    .contains(newRefreshToken);
        }

        @Test
        @DisplayName("저장된 리프레시 토큰이 없으면 재발급을 하지 않는다.")
        void reissue_fail_when_not_found() {
            // given
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            // when
            boolean result =
                    refreshTokenRepository.rotate(
                            userId,
                            sessionId,
                            "refreshToken",
                            "newRefreshToken");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("동일한 리프레시 토큰을 동시에 재발급하면 하나만 성공한다.")
        void reissue_only_one_success() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String refreshToken = "refreshToken";
            String newRefreshToken = "newRefreshToken";

            refreshTokenRepository.save(userId, sessionId, refreshToken);

            int threadCount = 10;
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger();

            // when
            ConcurrencyTestingUtil.run(
                    threadCount,
                    () -> {
                        boolean result =
                                refreshTokenRepository.rotate(
                                        userId,
                                        sessionId,
                                        refreshToken,
                                        newRefreshToken);

                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    });

            // then
            Assertions.assertThat(successCount.get()).isEqualTo(1);
            Assertions.assertThat(failureCount.get()).isEqualTo(9);

            Assertions.assertThat(refreshTokenRepository.findByUserId(userId, sessionId))
                    .contains(newRefreshToken);
        }
    }

    @Test
    @DisplayName("리프레시 토큰 삭제에 성공한다.")
    void delete_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String refreshToken = "refreshToken";

        refreshTokenRepository.save(userId, sessionId, refreshToken);

        // when
        refreshTokenRepository.delete(userId, sessionId);

        // then
        assertThat(refreshTokenRepository.findByUserId(userId, sessionId))
                .isEmpty();
    }
}
