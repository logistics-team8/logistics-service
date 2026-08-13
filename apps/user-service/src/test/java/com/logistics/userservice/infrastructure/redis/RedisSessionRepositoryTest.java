package com.logistics.userservice.infrastructure.redis;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.logistics.userservice.config.test.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@DisplayName("RedisSessionRepositoryTest - 통합 테스트")
class RedisSessionRepositoryTest extends AbstractIntegrationTest {
    @Autowired private RedisSessionRepository sessionRepository;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("Redis 저장 이후 조회 성공.")
    void save_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        // when
        sessionRepository.save(userId, sessionId, 1);

        // then
        assertThat(sessionRepository.exists(userId, sessionId)).isTrue();
    }

    @Nested
    @DisplayName("동시 로그인 테스트")
    class Session {
        @Test
        @DisplayName("세션 사이즈 초과 시 가장 오래된 세션을 삭제한다.")
        void save_fifo_success() throws InterruptedException {
            // given
            UUID userId = UUID.randomUUID();
            UUID oldSessionId = UUID.randomUUID();
            UUID newSessionId = UUID.randomUUID();

            sessionRepository.save(userId, oldSessionId, 1);

            Thread.sleep(10);

            // when
            sessionRepository.save(userId, newSessionId, 1);

            // then
            assertThat(sessionRepository.exists(userId, oldSessionId)).isFalse();
            assertThat(sessionRepository.exists(userId, newSessionId)).isTrue();
        }

        @Test
        @DisplayName("세션 사이즈를 초과해서 저장 불가능 하다.")
        void retain_maximum_sessions() throws InterruptedException {
            // given
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionId2 = UUID.randomUUID();
            UUID sessionId3 = UUID.randomUUID();

            sessionRepository.save(userId, sessionId, 2);
            Thread.sleep(10);

            sessionRepository.save(userId, sessionId2, 2);
            Thread.sleep(10);

            // when
            sessionRepository.save(userId, sessionId3, 2);

            // then
            assertThat(sessionRepository.exists(userId, sessionId)).isFalse();
            assertThat(sessionRepository.exists(userId, sessionId2)).isTrue();
            assertThat(sessionRepository.exists(userId, sessionId3)).isTrue();
        }
    }

    @Nested
    @DisplayName("세션 삭제 테스트")
    class Delete {
        @Test
        @DisplayName("특정 세션을 삭제한다.")
        void delete_success() {
            // given
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionId2 = UUID.randomUUID();

            sessionRepository.save(userId, sessionId, 2);
            sessionRepository.save(userId, sessionId2, 2);

            // when
            sessionRepository.delete(userId, sessionId);

            // then
            assertThat(sessionRepository.exists(userId, sessionId)).isFalse();
            assertThat(sessionRepository.exists(userId, sessionId2)).isTrue();
        }

        @Test
        @DisplayName("전체 세션을 삭제한다.")
        void delete_all_success() {
            // given
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID sessionId2 = UUID.randomUUID();

            sessionRepository.save(userId, sessionId, 2);
            sessionRepository.save(userId, sessionId2, 2);

            // when
            sessionRepository.deleteAll(userId);

            // then
            assertThat(sessionRepository.exists(userId, sessionId)).isFalse();
            assertThat(sessionRepository.exists(userId, sessionId2)).isFalse();
        }
    }
}
