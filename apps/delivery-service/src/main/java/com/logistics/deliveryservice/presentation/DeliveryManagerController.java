package com.logistics.deliveryservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.deliveryservice.application.dto.CreateDeliveryManagerResponse;
import com.logistics.deliveryservice.application.service.DeliveryManagerService;
import com.logistics.deliveryservice.presentation.dto.CreateDeliveryManagerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/delivery-managers")
@RequiredArgsConstructor
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateDeliveryManagerResponse>> createDeliveryManager(
            @Valid @RequestBody CreateDeliveryManagerRequest request
    ) {
        // Command 객체 변환후 생성 결과 response에 담음
        CreateDeliveryManagerResponse response = deliveryManagerService.create(
                request.toCommand()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
