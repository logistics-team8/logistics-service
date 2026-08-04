package com.logistics.orderservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.orderservice.application.service.OrderCommandService;
import com.logistics.orderservice.application.service.OrderQueryService;
import com.logistics.orderservice.page.PageResponse;
import com.logistics.orderservice.presentation.dto.request.CreateOrderRequest;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import com.logistics.orderservice.presentation.dto.response.OrderDetailResponse;
import com.logistics.orderservice.presentation.dto.response.OrderSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(
            @RequestHeader("X-User-Id") UUID requesterId,
            @Valid @RequestBody CreateOrderRequest request

    ) {
            CreateOrderResponse response =
                    orderCommandService.createOrder(request.toCommand(requesterId));
            return ApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @PathVariable UUID orderId
    ){
        OrderDetailResponse response = orderQueryService.getOrder(orderId);
        return ApiResponse.success(response);
    }


    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderSummaryResponse> page = orderQueryService.getOrders(pageable);
        return ApiResponse.success(PageResponse.from(page));
    }

}
