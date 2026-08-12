package com.logistics.deliveryservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.deliveryservice.application.dto.DeliveryManagerDetailResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerSearchResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerUpdateResponse;
import com.logistics.deliveryservice.application.service.DeliveryManagerService;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerSearchRequest;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerUpdateRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/delivery/managers")
@RequiredArgsConstructor
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeliveryManagerSearchResponse>>> searchDeliveryManagers(
            // SecurityContext에 저장된 로그인 사용자 정보
            @AuthenticationPrincipal CustomUserDetails userDetails,
            // Query Parameter를 검색 조건 DTO로 묶음(reques)
            @ModelAttribute DeliveryManagerSearchRequest request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        PageResponse.from(
                                deliveryManagerService.search(request, pageable, userDetails)
                        )
                )
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<DeliveryManagerDetailResponse>> getDeliveryManager(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryManagerService.getByUserId(userId, userDetails))
        );
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<DeliveryManagerUpdateResponse>> updateDeliveryManager(
            @PathVariable UUID userId,
            @RequestBody DeliveryManagerUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryManagerService.update(userId, request, userDetails))
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryManager(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        deliveryManagerService.delete(userId, userDetails);
        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}
