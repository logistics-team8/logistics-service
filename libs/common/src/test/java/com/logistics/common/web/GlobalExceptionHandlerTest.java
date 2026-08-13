package com.logistics.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.ValidationError;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("전역 예외 처리기")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("비즈니스 예외를 지정된 오류 코드 응답으로 변환한다")
    void converts_business_exception_to_its_error_code_response() throws Exception {
        mockMvc.perform(get("/business").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_202"))
                .andExpect(jsonPath("$.error.message").value("이미 존재하는 리소스입니다."))
                .andExpect(jsonPath("$.error.errors").isEmpty());
    }

    @Test
    @DisplayName("요청 본문 검증 오류를 필드 오류 응답으로 변환한다")
    void converts_request_body_validation_errors_to_field_error_response() throws Exception {
        mockMvc.perform(post("/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_002"))
                .andExpect(jsonPath("$.error.errors[0].field").value("name"))
                .andExpect(jsonPath("$.error.errors[0].message").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("잘못된 JSON을 잘못된 요청 응답으로 변환한다")
    void converts_malformed_json_to_invalid_input_response() throws Exception {
        mockMvc.perform(post("/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드를 405 응답으로 변환한다")
    void converts_unsupported_http_method_to_method_not_allowed_response() throws Exception {
        mockMvc.perform(get("/validation").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_003"))
                .andExpect(jsonPath("$.error.message").value("허용되지 않은 HTTP 메서드입니다."));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type을 415 응답으로 변환한다")
    void converts_unsupported_media_type_to_unsupported_media_type_response() throws Exception {
        mockMvc.perform(post("/validation")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_004"))
                .andExpect(jsonPath("$.error.message").value("지원하지 않는 Content-Type입니다."));
    }

    @Test
    @DisplayName("인증 예외를 401 응답으로 변환한다")
    void converts_authentication_exception_to_unauthorized_response() throws Exception {
        mockMvc.perform(get("/unauthorized").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_101"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("접근 거부 예외를 403 응답으로 변환한다")
    void converts_access_denied_exception_to_forbidden_response() throws Exception {
        mockMvc.perform(get("/forbidden").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_102"))
                .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("없는 리소스 요청을 404 응답으로 변환한다")
    void converts_missing_resource_to_not_found_response() throws Exception {
        mockMvc.perform(get("/missing").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_201"))
                .andExpect(jsonPath("$.error.message").value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 내부 정보를 노출하지 않는다")
    void converts_unexpected_exception_without_exposing_its_message() throws Exception {
        mockMvc.perform(get("/unexpected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_999"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive"))));
    }

    @Test
    @DisplayName("요청 파라미터 타입 불일치를 잘못된 요청 응답으로 변환한다")
    void converts_request_parameter_type_mismatch_to_invalid_input_response() throws Exception {
        mockMvc.perform(get("/number").param("size", "invalid").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("필수 요청 파라미터 누락을 잘못된 요청 응답으로 변환한다")
    void converts_missing_required_parameter_to_invalid_input_response() throws Exception {
        mockMvc.perform(get("/required").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("메서드 파라미터 검증 오류를 필드 오류 응답으로 변환한다")
    void converts_method_parameter_validation_to_validation_error_response() throws Exception {
        mockMvc.perform(get("/constrained").param("size", "0").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_002"))
                .andExpect(jsonPath("$.error.errors[0].field").value("size"));
    }

    @Test
    @DisplayName("제약 조건 위반 예외를 필드 오류 응답으로 변환한다")
    void converts_constraint_violation_to_validation_error_response() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(new ConstraintRequest(""));

        var response = new GlobalExceptionHandler()
                .handleConstraintViolationException(new ConstraintViolationException(violations));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().code()).isEqualTo("COMMON_002");
        assertThat(response.getBody().getError().errors())
                .containsExactly(new ValidationError("name", "이름은 필수입니다."));
    }

    @Test
    @DisplayName("필수 요청 헤더 누락을 잘못된 요청 응답으로 변환한다")
    void converts_missing_required_header_to_invalid_input_response() throws Exception {
        mockMvc.perform(get("/required-header").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("잘못된 요청입니다."));
    }

    @RestController
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new InsufficientAuthenticationException("not exposed");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new AccessDeniedException("not exposed");
        }

        @GetMapping("/missing")
        void missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/missing", "not found");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("sensitive internal detail");
        }

        @GetMapping("/number")
        void number(@RequestParam("size") int size) {
        }

        @GetMapping("/required")
        void required(@RequestParam("name") String name) {
        }

        @GetMapping("/constrained")
        void constrained(@RequestParam("size") @Min(1) int size) {
        }

        @GetMapping("/required-header")
        void requiredHeader(@RequestHeader("X-User-Id") String userId) {
        }
    }

    record ValidationRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }

    record ConstraintRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }
}
