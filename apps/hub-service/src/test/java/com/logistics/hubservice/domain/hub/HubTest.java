package com.logistics.hubservice.domain.hub;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HubTest {

    @Test
    void updatePreservesValuesThatAreOmitted() {
        Hub hub = Hub.create(
                "서울 허브",
                "서울특별시 송파구",
                new BigDecimal("37.514575"),
                new BigDecimal("127.112245")
        );

        hub.update("동서울 허브", null, null, new BigDecimal("127.120000"));

        assertThat(hub.getName()).isEqualTo("동서울 허브");
        assertThat(hub.getAddress()).isEqualTo("서울특별시 송파구");
        assertThat(hub.getLatitude()).isEqualByComparingTo("37.514575");
        assertThat(hub.getLongitude()).isEqualByComparingTo("127.120000");
    }

    @Test
    void deleteRecordsTheActor() {
        Hub hub = Hub.create(
                "서울 허브",
                "서울특별시 송파구",
                new BigDecimal("37.514575"),
                new BigDecimal("127.112245")
        );
        UUID deletedBy = UUID.fromString("7f8bfb51-5c00-42a8-a711-6d4c0b829f95");

        hub.delete(deletedBy);

        assertThat(hub.getDeletedAt()).isNotNull();
        assertThat(hub.getDeletedBy()).isEqualTo(deletedBy);
    }
}
