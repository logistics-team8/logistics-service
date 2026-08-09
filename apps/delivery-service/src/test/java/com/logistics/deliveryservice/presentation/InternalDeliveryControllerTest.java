package com.logistics.deliveryservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.common.web.GlobalExceptionHandler;
import com.logistics.deliveryservice.application.command.CreateDeliveryCommand;
import com.logistics.deliveryservice.application.dto.CreateDeliveryResponse;
import com.logistics.deliveryservice.application.dto.CreateDeliveryResult;
import com.logistics.deliveryservice.application.service.CreateDeliveryService;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import com.logistics.deliveryservice.domain.model.RouteStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalDeliveryController.class)
@Import(GlobalExceptionHandler.class)
class InternalDeliveryControllerTest {

    private static final UUID DELIVERY_ID = UUID.fromString("c7155435-8ba5-47e2-b0b0-285948878f5c");
    private static final UUID ORDER_ID = UUID.fromString("c9043ac7-ddf2-4592-8be2-1191fc5e090a");
    private static final UUID REQUESTER_ID = UUID.fromString("085ec814-24e5-4193-b9ea-65c37a0daff5");
    private static final UUID DEPARTURE_HUB_ID = UUID.fromString("8510f507-28bd-472a-980b-6ff08c2c8b66");
    private static final UUID ARRIVAL_HUB_ID = UUID.fromString("b5cd1365-dd46-4f12-955a-2bd986b85fae");
    private static final UUID COMPANY_MANAGER_ID = UUID.fromString("bd0a4bb4-c25d-4111-ad2f-a23c5f41d530");
    private static final UUID ROUTE_ID = UUID.fromString("d91ae0be-1d53-47f2-88d8-10134181270e");
    private static final UUID HUB_MANAGER_ID = UUID.fromString("1571df79-6e99-4555-a4b2-730109976ab1");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDeliveryService createDeliveryService;

    @Test
    void returnsCreatedForFirstRequest() throws Exception {
        when(createDeliveryService.create(any(CreateDeliveryCommand.class)))
                .thenReturn(CreateDeliveryResult.created(response()));

        mockMvc.perform(post("/internal/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deliveryId").value(DELIVERY_ID.toString()))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("HUB_WAITING"))
                .andExpect(jsonPath("$.data.routes[0].sequence").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(createDeliveryService).create(any(CreateDeliveryCommand.class));
    }

    @Test
    void returnsOkForIdempotentRequest() throws Exception {
        when(createDeliveryService.create(any(CreateDeliveryCommand.class)))
                .thenReturn(CreateDeliveryResult.existing(response()));

        mockMvc.perform(post("/internal/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void returnsValidationErrorForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/internal/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": null,
                                  "requesterId": null,
                                  "departureHubId": null,
                                  "arrivalHubId": null,
                                  "deliveryAddress": " ",
                                  "receiverName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("COMMON_002"))
                .andExpect(jsonPath("$.error.errors.length()").value(6));
    }

    @ParameterizedTest
    @MethodSource("businessErrors")
    void returnsBusinessError(DeliveryErrorCode errorCode, int expectedStatus) throws Exception {
        when(createDeliveryService.create(any(CreateDeliveryCommand.class)))
                .thenThrow(new DeliveryException(errorCode));

        mockMvc.perform(post("/internal/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(errorCode.code()))
                .andExpect(jsonPath("$.error.message").value(errorCode.message()));
    }

    private static Stream<Arguments> businessErrors() {
        return Stream.of(
                Arguments.of(DeliveryErrorCode.DUPLICATE_ORDER_DELIVERY, 409),
                Arguments.of(DeliveryErrorCode.INVALID_HUB_DELIVERY_PLAN, 400),
                Arguments.of(DeliveryErrorCode.HUB_DELIVERY_PLAN_UNAVAILABLE, 503)
        );
    }

    private String validRequest() {
        return """
                {
                  "orderId": "%s",
                  "requesterId": "%s",
                  "departureHubId": "%s",
                  "arrivalHubId": "%s",
                  "deliveryAddress": "서울시 중구 세종대로 1",
                  "receiverName": "홍길동",
                  "receiverSlackId": "receiver"
                }
                """.formatted(
                ORDER_ID,
                REQUESTER_ID,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID
        );
    }

    private CreateDeliveryResponse response() {
        return new CreateDeliveryResponse(
                DELIVERY_ID,
                ORDER_ID,
                REQUESTER_ID,
                DeliveryStatus.HUB_WAITING,
                DEPARTURE_HUB_ID,
                ARRIVAL_HUB_ID,
                "서울시 중구 세종대로 1",
                "홍길동",
                "receiver",
                COMPANY_MANAGER_ID,
                null,
                null,
                List.of(new CreateDeliveryResponse.RouteResponse(
                        ROUTE_ID,
                        1,
                        DEPARTURE_HUB_ID,
                        ARRIVAL_HUB_ID,
                        new BigDecimal("12.5"),
                        30,
                        RouteStatus.WAITING,
                        HUB_MANAGER_ID
                ))
        );
    }
}
