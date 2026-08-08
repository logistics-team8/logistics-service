package com.logistics.hubservice.application.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.test.util.ReflectionTestUtils;

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
    void getAllReturnsOnlyActiveHubsInCreatedAtDescendingOrder() {
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

        List<HubResponse> responses = service.getAll();

        assertThat(responses)
                .extracting(HubResponse::hubId)
                .containsExactly(newerHub.getId(), olderHub.getId());
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
            if (hub.getId() == null) {
                int index = sequence.incrementAndGet();
                ReflectionTestUtils.setField(hub, "id", new UUID(0L, index));
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
        public List<Hub> findAllByDeletedAtIsNullOrderByCreatedAtDesc() {
            return hubs.values().stream()
                    .filter(hub -> hub.getDeletedAt() == null)
                    .sorted(Comparator.comparing(Hub::getCreatedAt).reversed())
                    .toList();
        }
    }
}
