package com.logistics.orderservice.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(
        validatedBy = AtLeastDaysInFutureValidator.class
)
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastDaysInFuture {

    String message()
    default "일시는 현재로부터 최소 {days}일 이후여야 합니다.";

    int days() default 1;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
