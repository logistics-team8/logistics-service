package com.logistics.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

@DisplayName("공통 웹 예외 처리 자동 설정")
class CommonWebExceptionAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonWebExceptionAutoConfiguration.class));

    @Test
    @DisplayName("Servlet 웹 애플리케이션에 전역 예외 처리기를 등록한다")
    void registers_global_exception_handler_for_servlet_web_application() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    @DisplayName("사용자 정의 전역 예외 처리기가 있으면 자동 등록하지 않는다")
    void backs_off_when_application_provides_its_own_handler() {
        contextRunner
                .withBean(CustomGlobalExceptionHandler.class, CustomGlobalExceptionHandler::new)
                .run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    @DisplayName("웹 애플리케이션이 아니면 전역 예외 처리기를 등록하지 않는다")
    void does_not_register_handler_for_non_web_application() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CommonWebExceptionAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    static class CustomGlobalExceptionHandler extends GlobalExceptionHandler {
    }
}
