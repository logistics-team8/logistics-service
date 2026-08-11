package com.logistics.hubservice.application.hubroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.application.hubroute.command.CreateHubRouteCommand;
import com.logistics.hubservice.application.hubroute.command.HubRouteCommandService;
import com.logistics.hubservice.application.hubroute.command.UpdateHubRouteCommand;
import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class HubRouteCommandServiceTest {

    private static final UUID SOURCE_HUB_ID =
            UUID.fromString("01b6e9a4-5d93-4c22-b7ce-cb2f60c403d6");
    private static final UUID DESTINATION_HUB_ID =
            UUID.fromString("b44a6de8-51ae-4f34-b3ad-a484ae85583c");
    private static final UUID DELETED_BY =
            UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private InMemoryHubRepository hubRepository;
    private InMemoryHubRouteRepository hubRouteRepository;
    private HubRouteCommandService service;

    @BeforeEach
    void setUp() {
        hubRepository = new InMemoryHubRepository();
        hubRouteRepository = new InMemoryHubRouteRepository();
        service = new HubRouteCommandService(hubRepository, hubRouteRepository);
        hubRepository.add(activeHub(SOURCE_HUB_ID, "서울 허브"));
        hubRepository.add(activeHub(DESTINATION_HUB_ID, "대전 허브"));
    }

    @Test
    @DisplayName("활성 허브 간 방향성 경로를 생성한다")
    void createReturnsTheSavedDirectionalRoute() {
        HubRouteResponse response = service.create(new CreateHubRouteCommand(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        ));

        assertThat(response.hubRouteId()).isNotNull();
        assertThat(response.sourceHubId()).isEqualTo(SOURCE_HUB_ID);
        assertThat(response.destinationHubId()).isEqualTo(DESTINATION_HUB_ID);
        assertThat(response.distanceMeters()).isEqualTo(123_400L);
        assertThat(response.durationSeconds()).isEqualTo(7_200L);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        assertThat(hubRepository.lockedHubIds)
                .containsExactlyElementsOf(Stream.of(SOURCE_HUB_ID, DESTINATION_HUB_ID).sorted().toList());
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 동일하면 경로를 생성할 수 없다")
    void createRejectsTheSameSourceAndDestinationHubWithHub004() {
        assertBusinessException(
                () -> service.create(new CreateHubRouteCommand(
                        SOURCE_HUB_ID, SOURCE_HUB_ID, 1L, 1L)),
                HubErrorCode.HUB_ROUTE_SAME_HUB);
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 허브를 지정하면 경로를 생성할 수 없다")
    void createRejectsAMissingOrDeletedHubWithHub001() {
        Hub deletedDestination = activeHub(DESTINATION_HUB_ID, "대전 허브");
        deletedDestination.delete(UUID.randomUUID());
        hubRepository.add(deletedDestination);

        assertBusinessException(
                () -> service.create(new CreateHubRouteCommand(
                        SOURCE_HUB_ID, DESTINATION_HUB_ID, 1L, 1L)),
                HubErrorCode.HUB_NOT_FOUND);
    }

    @Test
    @DisplayName("동일한 방향의 활성 경로가 이미 있으면 경로를 중복 등록할 수 없다")
    void createRejectsAnActiveDuplicateWithHub003() {
        service.create(new CreateHubRouteCommand(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        ));

        assertBusinessException(
                () -> service.create(new CreateHubRouteCommand(
                        SOURCE_HUB_ID, DESTINATION_HUB_ID, 120_000L, 7_000L)),
                HubErrorCode.HUB_ROUTE_DUPLICATE);
    }

    @Test
    @DisplayName("생성 명령의 필수값과 양수 제약을 검증한다")
    void createCommandRejectsMissingOrNonPositiveValues() {
        assertThat(validator.validate(new CreateHubRouteCommand(
                null, DESTINATION_HUB_ID, 0L, -1L)))
                .hasSize(3);
    }

    @Test
    @DisplayName("활성 허브 경로의 입력된 이동 거리만 수정한다")
    void updateChangesOnlyTheSuppliedDistance() {
        HubRouteResponse createdRoute = service.create(new CreateHubRouteCommand(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        ));

        HubRouteResponse response = service.update(
                createdRoute.hubRouteId(),
                new UpdateHubRouteCommand(130_000L, null));

        assertThat(response.hubRouteId()).isEqualTo(createdRoute.hubRouteId());
        assertThat(response.sourceHubId()).isEqualTo(SOURCE_HUB_ID);
        assertThat(response.destinationHubId()).isEqualTo(DESTINATION_HUB_ID);
        assertThat(response.distanceMeters()).isEqualTo(130_000L);
        assertThat(response.durationSeconds()).isEqualTo(7_200L);
    }

    @Test
    @DisplayName("존재하지 않거나 논리 삭제된 허브 경로는 수정할 수 없다")
    void updateRejectsMissingOrDeletedRoute() {
        assertBusinessException(
                () -> service.update(UUID.randomUUID(), new UpdateHubRouteCommand(1L, null)),
                HubErrorCode.HUB_ROUTE_NOT_FOUND);

        HubRouteResponse createdRoute = service.create(new CreateHubRouteCommand(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        ));
        HubRoute deletedRoute = hubRouteRepository.routes.get(createdRoute.hubRouteId());
        ReflectionTestUtils.setField(deletedRoute, "deletedAt", LocalDateTime.now());

        assertBusinessException(
                () -> service.update(createdRoute.hubRouteId(), new UpdateHubRouteCommand(1L, null)),
                HubErrorCode.HUB_ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("수정할 이동 거리와 소요 시간이 모두 없으면 수정 명령을 검증할 수 없다")
    void updateCommandRequiresAtLeastOneSuppliedField() {
        assertThat(validator.validate(new UpdateHubRouteCommand(null, null)))
                .extracting(violation -> violation.getMessage())
                .containsExactly("수정할 이동 거리나 소요 시간을 하나 이상 입력해야 합니다.");
        assertThat(validator.validate(new UpdateHubRouteCommand(1L, null))).isEmpty();
        assertThat(validator.validate(new UpdateHubRouteCommand(null, 1L))).isEmpty();
    }

    @Test
    @DisplayName("활성 허브 경로를 삭제하면 요청자와 삭제 시각을 기록하고 활성 조회에서 제외한다")
    void deleteSoftDeletesActiveRoute() {
        HubRouteResponse createdRoute = service.create(new CreateHubRouteCommand(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        ));

        service.delete(createdRoute.hubRouteId(), DELETED_BY);

        HubRoute deletedRoute = hubRouteRepository.routes.get(createdRoute.hubRouteId());
        assertThat(deletedRoute.getDeletedAt()).isNotNull();
        assertThat(deletedRoute.getDeletedBy()).isEqualTo(DELETED_BY);
        assertThat(hubRouteRepository.findByIdAndDeletedAtIsNull(createdRoute.hubRouteId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않거나 이미 삭제된 허브 경로는 삭제할 수 없다")
    void deleteRejectsMissingOrDeletedRoute() {
        assertBusinessException(
                () -> service.delete(UUID.randomUUID(), DELETED_BY),
                HubErrorCode.HUB_ROUTE_NOT_FOUND);

        HubRouteResponse createdRoute = service.create(new CreateHubRouteCommand(
                SOURCE_HUB_ID,
                DESTINATION_HUB_ID,
                123_400L,
                7_200L
        ));
        service.delete(createdRoute.hubRouteId(), DELETED_BY);

        assertBusinessException(
                () -> service.delete(createdRoute.hubRouteId(), UUID.randomUUID()),
                HubErrorCode.HUB_ROUTE_NOT_FOUND);
    }

    private static void assertBusinessException(Runnable action, HubErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(expectedErrorCode));
    }

    private static Hub activeHub(UUID id, String name) {
        Hub hub = Hub.create(
                name,
                "주소",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        );
        ReflectionTestUtils.setField(hub, "id", id);
        return hub;
    }

    private static final class InMemoryHubRepository implements HubRepository {

        private final Map<UUID, Hub> hubs = new LinkedHashMap<>();
        private final List<UUID> lockedHubIds = new ArrayList<>();

        void add(Hub hub) {
            hubs.put(hub.getId(), hub);
        }

        @Override
        public Hub save(Hub hub) {
            hubs.put(hub.getId(), hub);
            return hub;
        }

        @Override
        public Optional<Hub> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.ofNullable(hubs.get(id)).filter(hub -> hub.getDeletedAt() == null);
        }

        @Override
        public Optional<Hub> findByIdAndDeletedAtIsNullForUpdate(UUID id) {
            lockedHubIds.add(id);
            return findByIdAndDeletedAtIsNull(id);
        }

        @Override
        public boolean existsByIdAndDeletedAtIsNull(UUID id) {
            return findByIdAndDeletedAtIsNull(id).isPresent();
        }

        @Override
        public Page<Hub> findAllByDeletedAtIsNull(Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public Page<Hub> search(String keyword, Pageable pageable) {
            return Page.empty(pageable);
        }
    }

    private static final class InMemoryHubRouteRepository implements HubRouteRepository {

        private final Map<UUID, HubRoute> routes = new LinkedHashMap<>();
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public HubRoute save(HubRoute hubRoute) {
            if (hubRoute.getId() == null) {
                int index = sequence.incrementAndGet();
                ReflectionTestUtils.setField(hubRoute, "id", new UUID(0L, index));
                ReflectionTestUtils.setField(
                        hubRoute, "createdAt", LocalDateTime.of(2026, 8, 9, 9, 0).plusMinutes(index));
            }
            ReflectionTestUtils.setField(
                    hubRoute, "updatedAt", LocalDateTime.of(2026, 8, 9, 10, 0).plusMinutes(sequence.get()));
            routes.put(hubRoute.getId(), hubRoute);
            return hubRoute;
        }

        @Override
        public List<HubRoute> saveAll(List<HubRoute> hubRoutes) {
            hubRoutes.forEach(this::save);
            return hubRoutes;
        }

        @Override
        public Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.ofNullable(routes.get(id))
                    .filter(route -> route.getDeletedAt() == null);
        }

        @Override
        public List<HubRoute> findAllByHubIdAndDeletedAtIsNull(UUID hubId) {
            return routes.values().stream()
                    .filter(route -> route.getDeletedAt() == null)
                    .filter(route -> route.getSourceHubId().equals(hubId)
                            || route.getDestinationHubId().equals(hubId))
                    .toList();
        }

        @Override
        public Page<HubRoute> search(UUID sourceHubId, UUID destinationHubId, Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                UUID sourceHubId, UUID destinationHubId) {
            return routes.values().stream()
                    .anyMatch(route -> route.getDeletedAt() == null
                            && route.getSourceHubId().equals(sourceHubId)
                            && route.getDestinationHubId().equals(destinationHubId));
        }
    }
}
