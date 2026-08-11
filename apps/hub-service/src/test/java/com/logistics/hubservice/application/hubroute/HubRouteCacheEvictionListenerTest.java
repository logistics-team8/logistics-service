package com.logistics.hubservice.application.hubroute;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class HubRouteCacheEvictionListenerTest {

    @Test
    @DisplayName("허브 삭제 트랜잭션 커밋 후 연결 경로 캐시를 제거한다")
    void evictsConnectedRouteCachesAndAllPathCachesAfterCommit() throws NoSuchMethodException {
        RecordingHubRouteCacheEvictor cacheEvictor = new RecordingHubRouteCacheEvictor();
        HubRouteCacheEvictionListener listener = new HubRouteCacheEvictionListener(cacheEvictor);
        UUID firstRouteId = UUID.randomUUID();
        UUID secondRouteId = UUID.randomUUID();

        listener.evictDeletedHubRoutes(new HubRoutesDeletedEvent(List.of(firstRouteId, secondRouteId)));

        assertThat(cacheEvictor.evictedRouteIds).containsExactly(firstRouteId, secondRouteId);
        assertThat(cacheEvictor.allPathsEvicted).isTrue();
        Method listenerMethod = HubRouteCacheEvictionListener.class.getDeclaredMethod(
                "evictDeletedHubRoutes", HubRoutesDeletedEvent.class);
        assertThat(listenerMethod.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    private static final class RecordingHubRouteCacheEvictor extends HubRouteCacheEvictor {

        private final List<UUID> evictedRouteIds = new ArrayList<>();
        private boolean allPathsEvicted;

        @Override
        public void evictHubRouteById(UUID hubRouteId) {
            evictedRouteIds.add(hubRouteId);
        }

        @Override
        public void evictAllPaths() {
            allPathsEvicted = true;
        }
    }
}
