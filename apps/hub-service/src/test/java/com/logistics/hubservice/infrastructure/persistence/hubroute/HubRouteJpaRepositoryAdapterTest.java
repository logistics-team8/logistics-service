package com.logistics.hubservice.infrastructure.persistence.hubroute;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HubRouteJpaRepositoryAdapterTest extends PostgreSqlIntegrationTest {

    private static final UUID MASTER_ID =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
    private static final UUID MODIFIER_ID =
            UUID.fromString("c69b113d-0991-4d8c-b7d0-87bdfadd18ae");
    private static final UUID DELETER_ID =
            UUID.fromString("9e954d31-8045-49cd-a741-f186da531f8d");

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("delete from p_hub_routes");
        jdbcTemplate.update("delete from p_hubs");
        authenticate(MASTER_ID);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("경로를 저장하면 감사 필드를 기록하고 활성 방향성 중복을 조회한다")
    void savePersistsRouteAndFindsTheActiveDirectionalDuplicate() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");

        HubRoute savedRoute = hubRouteRepository.save(HubRoute.create(
                sourceHub.getId(),
                destinationHub.getId(),
                123_400L,
                7_200L
        ));

        assertThat(savedRoute.getId()).isNotNull();
        assertThat(savedRoute.getCreatedAt()).isNotNull();
        assertThat(savedRoute.getUpdatedAt()).isNotNull();
        assertThat(savedRoute.getCreatedBy()).isEqualTo(MASTER_ID);
        assertThat(savedRoute.getUpdatedBy()).isEqualTo(MASTER_ID);
        assertThat(hubRouteRepository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                sourceHub.getId(), destinationHub.getId()))
                .isTrue();
        assertThat(hubRouteRepository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                destinationHub.getId(), sourceHub.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("ID 조회에서 논리 삭제된 허브 경로를 제외한다")
    void findByIdReturnsOnlyActiveRoute() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute savedRoute = hubRouteRepository.save(HubRoute.create(
                sourceHub.getId(),
                destinationHub.getId(),
                123_400L,
                7_200L
        ));

        assertThat(hubRouteRepository.findByIdAndDeletedAtIsNull(savedRoute.getId()))
                .map(HubRoute::getId)
                .contains(savedRoute.getId());

        jdbcTemplate.update(
                "update p_hub_routes set deleted_at = current_timestamp where id = ?",
                savedRoute.getId());

        assertThat(hubRouteRepository.findByIdAndDeletedAtIsNull(savedRoute.getId())).isEmpty();
    }

    @Test
    @DisplayName("출발 허브와 도착 허브 조건으로 활성 경로를 페이지 조회한다")
    void searchFiltersActiveRoutesBySourceAndDestinationHub() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        Hub otherHub = saveHub("부산 허브");
        HubRoute matchingRoute = saveRoute(sourceHub.getId(), destinationHub.getId());
        saveRoute(sourceHub.getId(), otherHub.getId());
        saveRoute(otherHub.getId(), destinationHub.getId());
        HubRoute deletedRoute = saveRoute(otherHub.getId(), sourceHub.getId());
        jdbcTemplate.update(
                "update p_hub_routes set deleted_at = current_timestamp where id = ?",
                deletedRoute.getId());

        Page<HubRoute> result = hubRouteRepository.search(
                sourceHub.getId(),
                destinationHub.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(result.getContent())
                .extracting(HubRoute::getId)
                .containsExactly(matchingRoute.getId());
    }

    @Test
    @DisplayName("활성 허브 경로 전체 조회에서 논리 삭제된 경로를 제외한다")
    void findAllReturnsOnlyActiveRoutes() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        Hub otherHub = saveHub("부산 허브");
        HubRoute firstActiveRoute = saveRoute(sourceHub.getId(), destinationHub.getId());
        HubRoute secondActiveRoute = saveRoute(destinationHub.getId(), otherHub.getId());
        HubRoute deletedRoute = saveRoute(otherHub.getId(), sourceHub.getId());
        jdbcTemplate.update(
                "update p_hub_routes set deleted_at = current_timestamp where id = ?",
                deletedRoute.getId());

        List<HubRoute> activeRoutes = hubRouteRepository.findAllByDeletedAtIsNull();

        assertThat(activeRoutes)
                .extracting(HubRoute::getId)
                .containsExactlyInAnyOrder(firstActiveRoute.getId(), secondActiveRoute.getId());
    }

    @Test
    @DisplayName("허브 경로를 수정하면 변경값과 수정 감사 정보를 저장한다")
    void updatePersistsChangedValuesAndAuditMetadata() throws InterruptedException {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute savedRoute = saveRoute(sourceHub.getId(), destinationHub.getId());
        var originalUpdatedAt = savedRoute.getUpdatedAt();

        Thread.sleep(10);
        authenticate(MODIFIER_ID);
        savedRoute.update(130_000L, null);
        hubRouteRepository.save(savedRoute);

        HubRoute updatedRoute = hubRouteRepository
                .findByIdAndDeletedAtIsNull(savedRoute.getId())
                .orElseThrow();
        assertThat(updatedRoute.getSourceHubId()).isEqualTo(sourceHub.getId());
        assertThat(updatedRoute.getDestinationHubId()).isEqualTo(destinationHub.getId());
        assertThat(updatedRoute.getDistanceMeters()).isEqualTo(130_000L);
        assertThat(updatedRoute.getDurationSeconds()).isEqualTo(7_200L);
        assertThat(updatedRoute.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedRoute.getUpdatedBy()).isEqualTo(MODIFIER_ID);
    }

    @Test
    @DisplayName("허브 경로를 삭제하면 삭제 시각과 요청자 UUID를 저장하고 활성 조회에서 제외한다")
    void deletePersistsDeletionMetadataAndExcludesRouteFromActiveLookup() {
        Hub sourceHub = saveHub("서울 허브");
        Hub destinationHub = saveHub("대전 허브");
        HubRoute savedRoute = saveRoute(sourceHub.getId(), destinationHub.getId());

        authenticate(DELETER_ID);
        savedRoute.delete(DELETER_ID);
        hubRouteRepository.save(savedRoute);

        assertThat(hubRouteRepository.findByIdAndDeletedAtIsNull(savedRoute.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is not null from p_hub_routes where id = ?",
                Boolean.class,
                savedRoute.getId()))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_by from p_hub_routes where id = ?",
                UUID.class,
                savedRoute.getId()))
                .isEqualTo(DELETER_ID);
        assertThat(jdbcTemplate.queryForObject(
                "select updated_by from p_hub_routes where id = ?",
                UUID.class,
                savedRoute.getId()))
                .isEqualTo(DELETER_ID);
    }

    private Hub saveHub(String name) {
        return hubRepository.save(Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        ));
    }

    private HubRoute saveRoute(UUID sourceHubId, UUID destinationHubId) {
        return hubRouteRepository.save(HubRoute.create(
                sourceHubId,
                destinationHubId,
                123_400L,
                7_200L
        ));
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.from(userId, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
