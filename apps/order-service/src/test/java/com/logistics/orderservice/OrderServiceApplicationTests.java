package com.logistics.orderservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceApplicationTests {

    @Test
    @DisplayName("주문 서비스 애플리케이션 진입점이 존재한다")
    void applicationEntryPointExists() {
        assertThat(OrderServiceApplication.class).isNotNull();
    }
}
