package com.logistics.notificationservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.notificationservice.application.notificaion.OrderNotificationService;
import com.logistics.notificationservice.presentation.slack.dto.DispatchNotificationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/notifications")
public class NotificationController {

    private final OrderNotificationService orderNotificationService;


    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Void>> notifyOrderCreated(
            @RequestBody DispatchNotificationRequestDto request
    ) {

        orderNotificationService.notifyOrderCreated(request);

        return ResponseEntity
                .accepted()
                .body(ApiResponse.success(null));
    }
}
