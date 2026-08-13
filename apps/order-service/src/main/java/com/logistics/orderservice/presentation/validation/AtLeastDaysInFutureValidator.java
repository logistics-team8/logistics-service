package com.logistics.orderservice.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AtLeastDaysInFutureValidator
        implements ConstraintValidator<
                AtLeastDaysInFuture,
                LocalDateTime
                > {

    private final Clock clock;

    private int days;

    @Override
    public void initialize(
            AtLeastDaysInFuture annotation
    ) {
        this.days = annotation.days();
    }

    @Override
    public boolean isValid(
            LocalDateTime value,
            ConstraintValidatorContext context
    ) {
        // 필수값 검증은 @NotNull이 담당한다.
        if (value == null) {
            return true;
        }

        LocalDateTime minimumDateTime =
                LocalDateTime.now(clock)
                        .plusDays(days);

        // 정확히 N일 이후인 값도 허용한다.
        return !value.isBefore(minimumDateTime);
    }
}