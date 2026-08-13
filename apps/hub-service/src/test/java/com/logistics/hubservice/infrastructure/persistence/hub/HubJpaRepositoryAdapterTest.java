package com.logistics.hubservice.infrastructure.persistence.hub;

import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.hubservice.PostgreSqlIntegrationTest;
import com.logistics.hubservice.application.hubroute.initialization.DefaultHub;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Hub JPA 저장소 어댑터")
class HubJpaRepositoryAdapterTest extends PostgreSqlIntegrationTest {

    @Autowired
    private HubRepository hubRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savePersistsHubWithTheAuthenticatedUserAsAuditor() {
        UUID userId = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
        authenticate(userId);

        Hub savedHub = hubRepository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구",
                new BigDecimal("37.514575"),
                new BigDecimal("127.112245")
        ));

        assertThat(savedHub.getId()).isNotNull();
        assertThat(savedHub.getCreatedAt()).isNotNull();
        assertThat(savedHub.getUpdatedAt()).isNotNull();
        assertThat(savedHub.getCreatedBy()).isEqualTo(userId);
        assertThat(savedHub.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("인증 사용자가 없어도 기본 Hub를 고정 UUID와 시스템 감사 정보로 저장한다")
    void savePersistsHubWithFixedId() {
        SecurityContextHolder.clearContext();
        DefaultHub defaultHub = DefaultHub.SEOUL;

        Hub savedHub = hubRepository.save(Hub.createDefault(
                defaultHub.hubId(),
                defaultHub.hubName(),
                defaultHub.address(),
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")));

        assertThat(savedHub.getId()).isEqualTo(defaultHub.hubId());
        assertThat(savedHub.getCreatedBy())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(savedHub.getUpdatedBy())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(hubRepository.findByIdAndDeletedAtIsNull(defaultHub.hubId()))
                .map(Hub::getName)
                .contains(defaultHub.hubName());
    }

    @Test
    @DisplayName("단일 조회와 검색에서 논리 삭제된 허브를 제외한다")
    void activeQueriesExcludeSoftDeletedHubs() {
        UUID userId = UUID.fromString("c69b113d-0991-4d8c-b7d0-87bdfadd18ae");
        authenticate(userId);
        Hub activeHub = hubRepository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구",
                new BigDecimal("37.514575"),
                new BigDecimal("127.112245")
        ));
        Hub deletedHub = hubRepository.save(Hub.create(
                "부산 허브",
                "부산광역시 강서구",
                new BigDecimal("35.179554"),
                new BigDecimal("129.075642")
        ));
        deletedHub.delete(userId);
        hubRepository.save(deletedHub);

        assertThat(hubRepository.findByIdAndDeletedAtIsNull(activeHub.getId()))
                .map(Hub::getId)
                .contains(activeHub.getId());
        assertThat(hubRepository.findByIdAndDeletedAtIsNull(deletedHub.getId())).isEmpty();
        assertThat(hubRepository.existsByIdAndDeletedAtIsNull(activeHub.getId())).isTrue();
        assertThat(hubRepository.existsByIdAndDeletedAtIsNull(deletedHub.getId())).isFalse();
        assertThat(hubRepository.existsByIdAndDeletedAtIsNull(UUID.randomUUID())).isFalse();
        Page<Hub> activeHubs = hubRepository.findAllByDeletedAtIsNull(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertThat(activeHubs.getContent())
                .extracting(Hub::getId)
                .contains(activeHub.getId())
                .doesNotContain(deletedHub.getId());
    }

    @Test
    @DisplayName("이름 또는 주소를 대소문자 구분 없이 검색하고 삭제된 허브를 제외한다")
    void searchMatchesNameOrAddressIgnoringCaseAndExcludesDeletedHubs() {
        UUID userId = UUID.fromString("5136e949-d047-4f31-8da2-e9654dd80f38");
        authenticate(userId);
        Hub nameMatch = hubRepository.save(Hub.create(
                "SEOUL Hub",
                "Korea",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));
        Hub addressMatch = hubRepository.save(Hub.create(
                "Busan Hub",
                "SeOuL Road 55",
                new BigDecimal("35.1795540"),
                new BigDecimal("129.0756420")
        ));
        Hub deletedMatch = hubRepository.save(Hub.create(
                "Seoul Deleted Hub",
                "Korea",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.1000000")
        ));
        deletedMatch.delete(userId);
        hubRepository.save(deletedMatch);

        Page<Hub> result = hubRepository.search(
                "seoul",
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(result.getContent())
                .extracting(Hub::getId)
                .containsExactlyInAnyOrder(addressMatch.getId(), nameMatch.getId())
                .doesNotContain(deletedMatch.getId());
    }

    @Test
    void reloadedHubRetainsCoordinatesWithSevenFractionalDigits() {
        UUID userId = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
        authenticate(userId);
        Hub savedHub = hubRepository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구",
                new BigDecimal("37.5145751"),
                new BigDecimal("127.1122451")
        ));

        Hub reloadedHub = hubRepository.findByIdAndDeletedAtIsNull(savedHub.getId()).orElseThrow();

        assertThat(reloadedHub.getLatitude()).isEqualByComparingTo("37.5145751");
        assertThat(reloadedHub.getLongitude()).isEqualByComparingTo("127.1122451");
    }

    @Test
    void updateChangesAuditMetadataToTheCurrentAuthenticatedUser() throws InterruptedException {
        UUID creatorId = UUID.fromString("e81cce60-2e94-41cd-9b89-dbf7dfc5f9b5");
        UUID modifierId = UUID.fromString("c69b113d-0991-4d8c-b7d0-87bdfadd18ae");
        authenticate(creatorId);
        Hub savedHub = hubRepository.save(Hub.create(
                "서울 허브",
                "서울특별시 송파구",
                new BigDecimal("37.514575"),
                new BigDecimal("127.112245")
        ));
        var originalUpdatedAt = savedHub.getUpdatedAt();

        Thread.sleep(10);
        authenticate(modifierId);
        savedHub.update("동서울 허브", null, null, null);
        hubRepository.save(savedHub);

        Hub updatedHub = hubRepository.findByIdAndDeletedAtIsNull(savedHub.getId()).orElseThrow();
        assertThat(updatedHub.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedHub.getUpdatedBy()).isEqualTo(modifierId);
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.from(userId, null, null, "MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
