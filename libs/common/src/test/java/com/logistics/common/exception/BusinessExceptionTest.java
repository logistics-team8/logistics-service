package com.logistics.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.error.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessException")
class BusinessExceptionTest {

    @Test
    @DisplayName("오류 코드와 해당 메시지를 보존한다")
    void exposes_the_error_code_without_using_an_arbitrary_client_message() {
        BusinessException exception = new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);

        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE);
        assertThat(exception.getMessage()).isEqualTo("이미 존재하는 리소스입니다.");
    }
}
