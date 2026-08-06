package com.logistics.infrastructure.config.test;

import com.logistics.userservice.infrastructure.security.JwtProperties;
import com.logistics.userservice.infrastructure.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** 추상 컨트롤러 테스트 클래스 */
@WebMvcTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableConfigurationProperties(JwtProperties.class)
@Import(SecurityConfig.class)
public abstract class AbstractControllerTest {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected JsonMapper jsonMapper;
}
