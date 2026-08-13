package com.logistics.deliveryservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.deliveryservice.application.dto.DeliveryDetailResponse;
import com.logistics.deliveryservice.application.dto.DeliveryRouteHistoryResponse;
import com.logistics.deliveryservice.application.dto.DeliverySearchResponse;
import com.logistics.deliveryservice.application.service.DeliveryService;
import com.logistics.deliveryservice.presentation.dto.DeliverySearchRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeliverySearchResponse>>> searchDeliveries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute DeliverySearchRequest request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        PageResponse.from(deliveryService.search(request, pageable, userDetails))
                )
        );
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<DeliveryDetailResponse>> getDelivery(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryService.getByDeliveryId(deliveryId, userDetails))
        );
    }

    @GetMapping("/{deliveryId}/routes")
    public ResponseEntity<ApiResponse<List<DeliveryRouteHistoryResponse>>> getDeliveryRoutes(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryService.getRoutesByDeliveryId(deliveryId, userDetails))
        );
    }

    @DeleteMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDelivery(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        deliveryService.delete(deliveryId, userDetails);
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}
