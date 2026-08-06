package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.UserService;
import com.logistics.userservice.presentation.dto.request.SignUpRequest;
import com.logistics.userservice.presentation.swagger.UserApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserApi {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody SignUpRequest request) {
        log.info("[UserController] User 회원가입 시작");
        userService.createUser(request.toCommand());
        log.info("[UserController] User 회원가입 완료");

        return ResponseEntity.status(HttpStatus.CREATED).body((ApiResponse.success(null)));
    }
}
