package com.logistics.orderservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class OrderServiceApplicationTests {

    @Test
    void applicationEntryPointExists() {
        assertThat(OrderServiceApplication.class)
                .isNotNull();
    }

}
