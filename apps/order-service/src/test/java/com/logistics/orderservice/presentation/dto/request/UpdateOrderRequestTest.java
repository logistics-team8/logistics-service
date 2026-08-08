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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateOrderRequest 검증")
class UpdateOrderRequestTest {
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
        UpdateOrderRequest request =
                new UpdateOrderRequest(
                        "변경 요청사항",
                        FIXED_NOW.plusDays(1)
                );

        Set<ConstraintViolation<UpdateOrderRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("희망 납품 일시가 1일보다 이전이면 검증에 실패한다")
    void lessThanOneDay_fail() {
        UpdateOrderRequest request =
                new UpdateOrderRequest(
                        "변경 요청사항",
                        FIXED_NOW.plusHours(23)
                );

        Set<ConstraintViolation<UpdateOrderRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath()
                                    .toString()
                    ).isEqualTo(
                            "requestedDeliveryAt"
                    );

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "일시는 현재로부터 최소 1일 이후여야 합니다."
                            );
                });
    }

    @Test
    @DisplayName("희망 납품 일시가 null이면 검증에 성공한다")
    void nullRequestedDeliveryAt_success() {
        UpdateOrderRequest request =
                new UpdateOrderRequest(
                        "요청사항만 변경",
                        null
                );

        Set<ConstraintViolation<UpdateOrderRequest>>
                violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("요청사항이 500자를 초과하면 검증에 실패한다")
    void requestMessageTooLong_fail() {
        UpdateOrderRequest request =
                new UpdateOrderRequest(
                        "가".repeat(501),
                        null
                );

        Set<ConstraintViolation<UpdateOrderRequest>>
                violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation.getPropertyPath()
                                    .toString()
                    ).isEqualTo("requestMessage");

                    assertThat(violation.getMessage())
                            .isEqualTo(
                                    "요청사항은 500자를 초과할 수 없습니다."
                            );
                });
    }
}