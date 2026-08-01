package com.logistics.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.error.CommonErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApiResponse")
class ApiResponseTest {

    @Test
    @DisplayName("성공 응답은 데이터만 포함한다")
    void success_response_contains_data_and_has_no_error() {
        ApiResponse<String> response = ApiResponse.success("hub");

        assertThat(response.getData()).isEqualTo("hub");
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("검증 실패 응답은 오류 코드와 필드 오류를 포함한다")
    void validation_failure_contains_common_code_and_field_errors() {
        List<ValidationError> errors = List.of(new ValidationError("name", "이름은 필수입니다."));

        ApiResponse<Void> response = ApiResponse.failure(CommonErrorCode.VALIDATION_ERROR, errors);

        assertThat(response.getData()).isNull();
        assertThat(response.getError().code()).isEqualTo("COMMON_002");
        assertThat(response.getError().message()).isEqualTo("입력값 검증에 실패했습니다.");
        assertThat(response.getError().errors()).containsExactlyElementsOf(errors);
    }

}
