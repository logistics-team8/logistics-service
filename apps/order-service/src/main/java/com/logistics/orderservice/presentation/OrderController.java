package com.logistics.orderservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.orderservice.application.service.OrderCommandService;
import com.logistics.orderservice.presentation.dto.request.CreateOrderRequest;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(
            @RequestHeader("X-User-Id") UUID requesterId,
            @Valid @RequestBody CreateOrderRequest request

    ) {
            CreateOrderResponse response =
                    orderCommandService.createOrder(request.toCommand(requesterId));
            return ApiResponse.success(response);
    }

}
