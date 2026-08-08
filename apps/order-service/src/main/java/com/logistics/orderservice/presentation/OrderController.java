package com.logistics.orderservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.orderservice.application.service.OrderCommandService;
import com.logistics.orderservice.application.service.OrderQueryService;
import com.logistics.orderservice.page.PageResponse;
import com.logistics.orderservice.presentation.dto.request.CreateOrderRequest;
import com.logistics.orderservice.presentation.dto.request.UpdateOrderRequest;
import com.logistics.orderservice.presentation.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 주문 단건 조회
     *
     * Security 적용되면 추가하겠습니다.
     * - MASTER: 전체 주문
     * - HUB_MANAGER: 본인 주문 또는 담당 허브 주문
     * - 그 외 로그인 사용자: 본인 주문
     */
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(orderQueryService.getOrder(orderId));
    }

    /**
     * 주문 목록 조회
     *
     * Security 적용되면 추가하겠습니다.
     * - MASTER: 전체 주문
     * - HUB_MANAGER: 본인 주문 또는 담당 허브 주문
     * - 그 외 로그인 사용자: 본인 주문
     */
    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderSummaryResponse> page = orderQueryService.getOrders(pageable);
        return ApiResponse.success(PageResponse.from(page));
    }


    /**
     * 주문 수정
     * 권한 추후 적용
     */
    @PatchMapping("/{orderId}")
    public ApiResponse<UpdateOrderResponse> updateOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateOrderRequest request,
            @PathVariable UUID orderId
    ){
        UpdateOrderResponse response = orderCommandService.updateOrder(userId, request.toCommand(), orderId);
        return ApiResponse.success(response);
    }


    /**
     * 주문 삭제
     * 권한 추후 적용
     */
    @DeleteMapping("/{orderId}")
    public ApiResponse<DeleteOrderResponse> deleteOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId
    ){
        return ApiResponse.success(orderCommandService.deleteOrder(userId, orderId));
    }

}
