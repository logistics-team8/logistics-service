package com.logistics.orderservice.presentation.dto.request;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateOrderRequest 검증")
class CreateOrderRequestTest {
    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private static final LocalDateTime FIXED_NOW =
            LocalDateTime.of(
                    2026,
                    8,
                    7,
                    10,
                    0
            );

    private AnnotationConfigApplicationContext context;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                FIXED_NOW.atZone(ZONE_ID).toInstant(),
                ZONE_ID
        );

        context =
                new AnnotationConfigApplicationContext();

        context.registerBean(
                Clock.class,
                () -> fixedClock
        );

        context.refresh();

        validator = new LocalValidatorFactoryBean();

        validator.setConstraintValidatorFactory(
                new SpringConstraintValidatorFactory(
                        context.getAutowireCapableBeanFactory()
                )
        );

        validator.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        validator.close();
        context.close();
    }

    @Test
    @DisplayName("희망 납품 일시가 정확히 1일 이후이면 검증에 성공한다")
    void exactlyOneDayLater_success() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(1),
                validItems()
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("희망 납품 일시가 1일보다 이전이면 검증에 실패한다")
    void lessThanOneDay_fail() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(1)
                        .minusNanos(1),
                validItems()
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "requestedDeliveryAt",
                "희망 납품 일시는 현재로부터 최소 1일 이후여야 합니다."
        );
    }

    @Test
    @DisplayName("희망 납품 일시가 1일 이후이면 검증에 성공한다")
    void moreThanOneDayLater_success() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(3),
                validItems()
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("희망 납품 일시가 null이면 필수값 검증에 실패한다")
    void nullRequestedDeliveryAt_fail() {
        CreateOrderRequest request = createRequest(
                null,
                validItems()
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "requestedDeliveryAt",
                "희망 납품 일시는 필수입니다."
        );

        // 커스텀 Validator는 null을 통과시키고
        // @NotNull만 실패해야 한다.
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("주문상품 리스트가 null이면 검증에 실패한다")
    void nullItems_fail() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(2),
                null
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "items",
                "주문상품은 1개 이상이어야 합니다."
        );
    }

    @Test
    @DisplayName("주문상품 리스트가 비어 있으면 검증에 실패한다")
    void emptyItems_fail() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(2),
                List.of()
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "items",
                "주문상품은 1개 이상이어야 합니다."
        );
    }

    @Test
    @DisplayName("주문상품 리스트에 null 요소가 있으면 검증에 실패한다")
    void nullItemElement_fail() {
        List<CreateOrderItemRequest> items =
                Arrays.asList(
                        (CreateOrderItemRequest) null
                );

        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(2),
                items
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "items[0].<list element>",
                "주문상품 정보는 필수입니다."
        );
    }

    @Test
    @DisplayName("주문상품의 상품 ID가 null이면 중첩 검증에 실패한다")
    void nullProductId_fail() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(2),
                List.of(
                        new CreateOrderItemRequest(
                                null,
                                1
                        )
                )
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "items[0].productId",
                "상품 ID는 필수입니다."
        );
    }

    @Test
    @DisplayName("주문상품 수량이 1보다 작으면 중첩 검증에 실패한다")
    void quantityLessThanOne_fail() {
        CreateOrderRequest request = createRequest(
                FIXED_NOW.plusDays(2),
                List.of(
                        new CreateOrderItemRequest(
                                UUID.randomUUID(),
                                0
                        )
                )
        );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "items[0].quantity",
                "주문 수량은 1개 이상이어야 합니다."
        );
    }

    @Test
    @DisplayName("요청사항이 500자를 초과하면 검증에 실패한다")
    void requestMessageTooLong_fail() {
        CreateOrderRequest request =
                new CreateOrderRequest(
                        UUID.randomUUID(),
                        "가".repeat(501),
                        FIXED_NOW.plusDays(2),
                        validItems()
                );

        Set<ConstraintViolation<CreateOrderRequest>>
                violations = validator.validate(request);

        assertViolation(
                violations,
                "requestMessage",
                "요청사항은 500자를 초과할 수 없습니다."
        );
    }

    private CreateOrderRequest createRequest(
            LocalDateTime requestedDeliveryAt,
            List<CreateOrderItemRequest> items
    ) {
        return new CreateOrderRequest(
                UUID.randomUUID(),
                "안전하게 배송해주세요.",
                requestedDeliveryAt,
                items
        );
    }

    private List<CreateOrderItemRequest> validItems() {
        return List.of(
                new CreateOrderItemRequest(
                        UUID.randomUUID(),
                        2
                )
        );
    }

    private void assertViolation(
            Set<? extends ConstraintViolation<?>>
                    violations,
            String propertyPath,
            String message
    ) {
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath()
                                    .toString()
                    ).isEqualTo(propertyPath);

                    assertThat(violation.getMessage())
                            .isEqualTo(message);
                });
    }

}