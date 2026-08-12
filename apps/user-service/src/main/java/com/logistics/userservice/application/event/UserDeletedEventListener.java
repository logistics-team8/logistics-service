package com.logistics.userservice.application.event;

import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.domain.redis.SessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedEventListener {
    private final SessionRepository sessionRepository;
    private final RoleCacheRepository roleCacheRepository;

    @Async
    @TransactionalEventListener
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        UUID userId = event.userId();

        try {
            sessionRepository.deleteAll(userId);
        } catch (Exception e) {
            log.warn("[FAIL] 회원 탈퇴 후 세션 정보 삭제 실패 userId = {}", userId, e);
        }

        try {
            roleCacheRepository.delete(userId);
        } catch (Exception e) {
            log.warn("[FAIL] 회원 탈퇴 후 권한 캐시 삭제 실패 userId = {}", userId, e);
        }
    }
}
