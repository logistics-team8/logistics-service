package com.logistics.orderservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.service.command.OrderCancelService;
import com.logistics.orderservice.application.service.command.OrderCommandService;
import com.logistics.orderservice.application.service.command.OrderCreateService;
import com.logistics.orderservice.application.service.query.OrderQueryService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderCreateService orderCreateService;
    private final OrderCancelService  orderCancelService;
    private final OrderQueryService orderQueryService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request

    ) {
        CreateOrderResponse response = orderCreateService.createOrder(request.toCommand(), userDetails);
        return ApiResponse.success(response);
    }

    /**
     * 주문 단건 조회
     * <p>
     * Security 적용되면 추가하겠습니다.
     * - MASTER: 전체 주문
     * - HUB_MANAGER: 본인 주문 또는 담당 허브 주문
     * - 그 외 로그인 사용자: 본인 주문
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(orderQueryService.getOrder(userDetails, orderId));
    }

    /**
     * 주문 목록 조회
     * <p>
     * Security 적용되면 추가하겠습니다.
     * - MASTER: 전체 주문
     * - HUB_MANAGER: 본인 주문 또는 담당 허브 주문
     * - 그 외 로그인 사용자: 본인 주문
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderSummaryResponse> page = orderQueryService.getOrders(userDetails, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }


    /**
     * 주문 수정
     * 권한 추후 적용
     */
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @PatchMapping("/{orderId}")
    public ApiResponse<UpdateOrderResponse> updateOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateOrderRequest request,
            @PathVariable UUID orderId
    ) {
        UpdateOrderResponse response = orderCommandService.updateOrder(userDetails, request.toCommand(), orderId);
        return ApiResponse.success(response);
    }


    /**
     * 주문 삭제
     * 권한 추후 적용
     */
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @DeleteMapping("/{orderId}")
    public ApiResponse<DeleteOrderResponse> deleteOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(orderCommandService.deleteOrder(userDetails, orderId));
    }


    /**
     * 주문 취소
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<CancelOrderResponse> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(orderCancelService.cancelOrder(userDetails, orderId));
    }


    /**
     * 주문 상품 취소
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{orderId}/items/{orderItemId}/cancel")
    public ApiResponse<CancelOrderItemResponse> cancelOrderItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID orderId,
            @PathVariable UUID orderItemId
    ){
        return ApiResponse.success(orderCancelService.cancelOrderItem(userDetails,orderId,orderItemId));
    }

}
