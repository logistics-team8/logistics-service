package com.logistics.orderservice;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceApplicationTests {

    @Test
    void applicationEntryPointExists() {
        AssertionsForClassTypes.assertThat(OrderServiceApplication.class)
                .isNotNull();
    }

}
