package com.logistics.userservice.application.event;

import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedEventListener {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleCacheRepository roleCacheRepository;

    @TransactionalEventListener
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        UUID userId = event.userId();

        try {
            refreshTokenRepository.delete(userId);
            roleCacheRepository.delete(userId);
        } catch (DataAccessException e) {
            log.warn("[FAIL] 회원 탈퇴 후 Redis 인증 정보 삭제 실패 userId = {}", userId, e);
        }
    }
}
