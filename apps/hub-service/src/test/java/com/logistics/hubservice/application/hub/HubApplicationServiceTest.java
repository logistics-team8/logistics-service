package com.logistics.hubservice.application.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.command.CreateHubCommand;
import com.logistics.hubservice.application.hub.command.HubCommandService;
import com.logistics.hubservice.application.hub.command.UpdateHubCommand;
import com.logistics.hubservice.application.hub.query.HubQueryService;
import com.logistics.hubservice.application.hub.dto.HubResponse;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Hub 애플리케이션 서비스")
class HubApplicationServiceTest {

    private static final UUID DELETED_BY = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createReturnsResponseReadyHubProjection() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        HubCommandService service = new HubCommandService(repository);

        HubResponse response = service.create(new CreateHubCommand(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));

        assertThat(response.hubId()).isNotNull();
        assertThat(response.name()).isEqualTo("서울 허브");
        assertThat(response.address()).isEqualTo("서울특별시 송파구 송파대로 55");
        assertThat(response.latitude()).isEqualByComparingTo("37.5145751");
        assertThat(response.longitude()).isEqualByComparingTo("127.1122451");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void updateChangesOnlySuppliedFieldsAndReturnsProjection() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        Hub existingHub = repository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        HubCommandService service = new HubCommandService(repository);

        HubResponse response = service.update(existingHub.getId(), new UpdateHubCommand(
                "동서울 허브",
                null,
                null,
                new BigDecimal("127.1200000")
        ));

        assertThat(response.hubId()).isEqualTo(existingHub.getId());
        assertThat(response.name()).isEqualTo("동서울 허브");
        assertThat(response.address()).isEqualTo("서울특별시 송파구 송파대로 55");
        assertThat(response.latitude()).isEqualByComparingTo("37.5145751");
        assertThat(response.longitude()).isEqualByComparingTo("127.1200000");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void deleteSoftDeletesAnActiveHub() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        Hub existingHub = repository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        HubCommandService service = new HubCommandService(repository);

        service.delete(existingHub.getId(), DELETED_BY);

        assertThat(existingHub.getDeletedAt()).isNotNull();
        assertThat(existingHub.getDeletedBy()).isEqualTo(DELETED_BY);
        assertThat(repository.findByIdAndDeletedAtIsNull(existingHub.getId())).isEmpty();
    }

    @Test
    void getOneReturnsTheStoredResponseProjection() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        Hub existingHub = repository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        HubQueryService service = new HubQueryService(repository);

        HubResponse response = service.getOne(existingHub.getId());

        assertThat(response.hubId()).isEqualTo(existingHub.getId());
        assertThat(response.name()).isEqualTo("서울 허브");
        assertThat(response.address()).isEqualTo("서울특별시 송파구 송파대로 55");
        assertThat(response.latitude()).isEqualByComparingTo("37.5145751");
        assertThat(response.longitude()).isEqualByComparingTo("127.1122451");
        assertThat(response.createdAt()).isEqualTo(existingHub.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(existingHub.getUpdatedAt());
    }

    @Test
    @DisplayName("공백 검색어는 전체 활성 허브로 처리하고 생성일 역순으로 반환한다")
    void searchReturnsOnlyActiveHubsInCreatedAtDescendingOrder() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        Hub olderHub = repository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        Hub newerHub = repository.save(Hub.create(
                "부산 허브",
                "부산광역시 동구 중앙대로 206",
                new BigDecimal("35.1795540"),
                new BigDecimal("129.0756420")
        ));
        Hub deletedHub = repository.save(Hub.create(
                "대구 허브",
                "대구광역시 북구 태평로 161",
                new BigDecimal("35.8714354"),
                new BigDecimal("128.6014450")
        ));
        deletedHub.delete(DELETED_BY);
        repository.save(deletedHub);
        HubQueryService service = new HubQueryService(repository);

        Page<HubResponse> responses = service.search(
                "   ",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(responses.getContent())
                .extracting(HubResponse::hubId)
                .containsExactly(newerHub.getId(), olderHub.getId());
    }

    @Test
    @DisplayName("검색어의 공백과 대소문자를 무시해 허브 이름 또는 주소를 검색한다")
    void searchNormalizesKeywordAndMatchesNameOrAddressIgnoringCase() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        Hub nameMatch = repository.save(Hub.create(
                "SEOUL Hub",
                "Korea",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        Hub addressMatch = repository.save(Hub.create(
                "Busan Hub",
                "SeOuL Road 55",
                new BigDecimal("35.1795540"),
                new BigDecimal("129.0756420")
        ));
        repository.save(Hub.create(
                "Daegu Hub",
                "Daegu Road 1",
                new BigDecimal("35.8714354"),
                new BigDecimal("128.6014450")
        ));
        HubQueryService service = new HubQueryService(repository);

        Page<HubResponse> responses = service.search(
                "  seOuL  ",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(responses.getContent())
                .extracting(HubResponse::hubId)
                .containsExactly(addressMatch.getId(), nameMatch.getId());
    }

    @Test
    @DisplayName("허용하지 않는 페이지 크기는 10으로 보정한다")
    void searchFallsBackToPageSizeTen() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        repository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        HubQueryService service = new HubQueryService(repository);

        Page<HubResponse> responses = service.search(
                null,
                PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(responses.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("지원하지 않는 정렬 필드는 잘못된 요청으로 거절한다")
    void searchRejectsUnsupportedSortProperty() {
        HubQueryService service = new HubQueryService(new InMemoryHubRepository());

        assertThatThrownBy(() -> service.search(null, PageRequest.of(0, 10, Sort.by("name"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void missingOrDeletedHubsProduceHub001AcrossActiveLookupUseCases() {
        InMemoryHubRepository repository = new InMemoryHubRepository();
        Hub deletedHub = repository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구 송파대로 55",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        deletedHub.delete(DELETED_BY);
        repository.save(deletedHub);
        HubCommandService commandService = new HubCommandService(repository);
        HubQueryService queryService = new HubQueryService(repository);
        UUID missingHubId = UUID.fromString("6f21f2ae-d913-45db-83e5-1a5695536171");

        assertHubNotFound(() -> queryService.getOne(missingHubId));
        assertHubNotFound(() -> queryService.getOne(deletedHub.getId()));
        assertHubNotFound(() -> commandService.update(
                deletedHub.getId(),
                new UpdateHubCommand("동서울 허브", null, null, null)
        ));
        assertHubNotFound(() -> commandService.delete(deletedHub.getId(), DELETED_BY));
    }

    @Test
    void updateCommandRequiresAtLeastOneSuppliedFieldWhileAllowingOtherFieldsToBeNull() {
        assertThat(validator.validate(new UpdateHubCommand(null, null, null, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateHubCommand(null, "부산광역시 동구 중앙대로 206", null, null)))
                .isEmpty();
    }

    @Test
    void updateCommandAcceptsTextThatContainsNonWhitespaceCharacters() {
        assertThat(validator.validate(new UpdateHubCommand("\n서울 허브", null, null, null))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void commandValidationRejectsInvalidSuppliedValues(Object command) {
        assertThat(validator.validate(command)).isNotEmpty();
    }

    private static Stream<Arguments> invalidCommands() {
        return Stream.of(
                Arguments.of(new CreateHubCommand(
                        " ",
                        "서울특별시 송파구 송파대로 55",
                        new BigDecimal("37.5145751"),
                        new BigDecimal("127.1122451")
                )),
                Arguments.of(new CreateHubCommand(
                        "허".repeat(101),
                        "서울특별시 송파구 송파대로 55",
                        new BigDecimal("37.5145751"),
                        new BigDecimal("127.1122451")
                )),
                Arguments.of(new CreateHubCommand(
                        "서울 허브",
                        "주".repeat(256),
                        new BigDecimal("37.5145751"),
                        new BigDecimal("127.1122451")
                )),
                Arguments.of(new CreateHubCommand(
                        "서울 허브",
                        "서울특별시 송파구 송파대로 55",
                        null,
                        new BigDecimal("127.1122451")
                )),
                Arguments.of(new CreateHubCommand(
                        "서울 허브",
                        "서울특별시 송파구 송파대로 55",
                        new BigDecimal("90.00000001"),
                        new BigDecimal("127.1122451")
                )),
                Arguments.of(new CreateHubCommand(
                        "서울 허브",
                        "서울특별시 송파구 송파대로 55",
                        new BigDecimal("37.5145751"),
                        new BigDecimal("180.00000001")
                )),
                Arguments.of(new UpdateHubCommand(" ", null, null, null)),
                Arguments.of(new UpdateHubCommand(null, null, new BigDecimal("37.51457511"), null))
        );
    }

    private static void assertHubNotFound(ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(HubErrorCode.HUB_NOT_FOUND);
                    assertThat(businessException.getErrorCode().code()).isEqualTo("HUB_001");
                });
    }

    private static final class InMemoryHubRepository implements HubRepository {

        private final Map<UUID, Hub> hubs = new LinkedHashMap<>();
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Hub save(Hub hub) {
            if (!hubs.containsKey(hub.getId())) {
                int index = sequence.incrementAndGet();
                ReflectionTestUtils.setField(hub, "createdAt", LocalDateTime.of(2026, 8, 5, 9, 0).plusMinutes(index));
            }
            ReflectionTestUtils.setField(hub, "updatedAt", LocalDateTime.of(2026, 8, 5, 10, 0).plusMinutes(sequence.get()));
            hubs.put(hub.getId(), hub);
            return hub;
        }

        @Override
        public Optional<Hub> findByIdAndDeletedAtIsNull(UUID id) {
            return Optional.ofNullable(hubs.get(id)).filter(hub -> hub.getDeletedAt() == null);
        }

        @Override
        public Page<Hub> findAllByDeletedAtIsNull(Pageable pageable) {
            return getPage(null, pageable);
        }

        @Override
        public Page<Hub> search(String keyword, Pageable pageable) {
            return getPage(keyword, pageable);
        }

        private Page<Hub> getPage(String keyword, Pageable pageable) {
            List<Hub> matchingHubs = new ArrayList<>(hubs.values().stream()
                    .filter(hub -> hub.getDeletedAt() == null)
                    .filter(hub -> matchesKeyword(hub, keyword))
                    .toList());

            Comparator<Hub> comparator = null;
            for (Sort.Order order : pageable.getSort()) {
                Comparator<Hub> nextComparator = comparatorFor(order);
                comparator = comparator == null ? nextComparator : comparator.thenComparing(nextComparator);
            }
            if (comparator != null) {
                matchingHubs.sort(comparator);
            }

            int start = (int) Math.min(pageable.getOffset(), matchingHubs.size());
            int end = Math.min(start + pageable.getPageSize(), matchingHubs.size());
            return new PageImpl<>(matchingHubs.subList(start, end), pageable, matchingHubs.size());
        }

        private boolean matchesKeyword(Hub hub, String keyword) {
            if (keyword == null) {
                return true;
            }
            return hub.getName().toLowerCase(Locale.ROOT).contains(keyword)
                    || hub.getAddress().toLowerCase(Locale.ROOT).contains(keyword);
        }

        private Comparator<Hub> comparatorFor(Sort.Order order) {
            Comparator<Hub> comparator = switch (order.getProperty()) {
                case "createdAt" -> Comparator.comparing(Hub::getCreatedAt);
                case "updatedAt" -> Comparator.comparing(Hub::getUpdatedAt);
                default -> throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다.");
            };
            return order.isDescending() ? comparator.reversed() : comparator;
        }
    }
}
