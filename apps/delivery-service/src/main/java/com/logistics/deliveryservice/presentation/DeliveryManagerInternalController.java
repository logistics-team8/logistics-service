package com.logistics.deliveryservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.deliveryservice.application.dto.DeliveryManagerCreateResponse;
import com.logistics.deliveryservice.application.service.DeliveryManagerService;
import com.logistics.deliveryservice.presentation.dto.DeliveryManagerCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/delivery-managers")
@RequiredArgsConstructor
public class DeliveryManagerInternalController {

    private final DeliveryManagerService deliveryManagerService;

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryManagerCreateResponse>> createDeliveryManager(
            @Valid @RequestBody DeliveryManagerCreateRequest request
    ) {
        DeliveryManagerCreateResponse response = deliveryManagerService.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
