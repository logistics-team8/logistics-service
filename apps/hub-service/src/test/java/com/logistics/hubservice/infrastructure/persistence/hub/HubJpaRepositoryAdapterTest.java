package com.logistics.hubservice.infrastructure.persistence.hub;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.security.CustomUserDetails;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HubJpaRepositoryAdapterTest {

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
        List<Hub> activeHubs = hubRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();
        assertThat(activeHubs)
                .extracting(Hub::getId)
                .contains(activeHub.getId())
                .doesNotContain(deletedHub.getId());
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.from(userId, "ROLE_MASTER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
