package com.logistics.hubservice.application.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.application.hub.command.HubCommandService;
import com.logistics.hubservice.application.hubroute.command.CreateHubRouteCommand;
import com.logistics.hubservice.application.hubroute.command.HubRouteCommandService;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Hub 삭제 동시성 통합 테스트")
class HubDeleteConcurrencyIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubCommandService hubCommandService;

    @Autowired
    private HubRouteCommandService hubRouteCommandService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
    }

    @Test
    @DisplayName("허브 삭제와 겹친 경로 생성은 삭제 커밋 후 거절한다")
    void routeCreationWaitsForHubDeletionAndThenFails() throws Exception {
        authenticate();
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        SecurityContextHolder.clearContext();

        CountDownLatch hubLocked = new CountDownLatch(1);
        CountDownLatch allowDeletion = new CountDownLatch(1);
        CountDownLatch routeCreationStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> deleteFuture = executor.submit(() -> deleteHubAfterLock(
                    sourceHub.getId(), hubLocked, allowDeletion));
            assertThat(hubLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> createRouteFuture = executor.submit(() -> createRoute(
                    sourceHub.getId(), destinationHub.getId(), routeCreationStarted));
            assertThat(routeCreationStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> createRouteFuture.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            allowDeletion.countDown();
            deleteFuture.get(5, TimeUnit.SECONDS);

            assertThatThrownBy(() -> createRouteFuture.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException cause = (BusinessException) exception.getCause();
                        assertThat(cause.getErrorCode()).isEqualTo(HubErrorCode.HUB_NOT_FOUND);
                    });
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from p_hub_routes", Long.class))
                    .isZero();
        } finally {
            allowDeletion.countDown();
            executor.shutdownNow();
            SecurityContextHolder.clearContext();
        }
    }

    private void deleteHubAfterLock(
            UUID hubId, CountDownLatch hubLocked, CountDownLatch allowDeletion) {
        authenticate();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                hubRepository.findByIdAndDeletedAtIsNullForUpdate(hubId).orElseThrow();
                hubLocked.countDown();
                await(allowDeletion);
                hubCommandService.delete(hubId, MASTER_ID);
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void createRoute(
            UUID sourceHubId, UUID destinationHubId, CountDownLatch routeCreationStarted) {
        authenticate();
        try {
            routeCreationStarted.countDown();
            hubRouteCommandService.create(new CreateHubRouteCommand(
                    sourceHubId,
                    destinationHubId,
                    123_400L,
                    7_200L
            ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Hub saveHub(String name) {
        return hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        ));
    }

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.from(MASTER_ID, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었습니다.", exception);
        }
    }
}
