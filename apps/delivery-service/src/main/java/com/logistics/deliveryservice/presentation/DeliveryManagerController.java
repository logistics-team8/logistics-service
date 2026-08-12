package com.logistics.deliveryservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.common.response.PageResponse;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.deliveryservice.application.dto.DeliveryManagerSearchResponse;
import com.logistics.deliveryservice.application.service.DeliveryManagerService;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/delivery-managers")
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
}
