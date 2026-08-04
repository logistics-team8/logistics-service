package com.logistics.userservice.presentation;

import com.logistics.userservice.application.UserService;
import com.logistics.userservice.application.dto.UserInfo;
import com.logistics.userservice.application.dto.UserRole;
import com.logistics.userservice.application.dto.UserSlackInfo;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserInfo getUserInfo(@PathVariable UUID userId) {
        return userService.getUserInfo(userId);
    }

    @GetMapping("/{userId}/role")
    public UserRole getUserRole(@PathVariable UUID userId) {
        return userService.getUserRole(userId);
    }

    @GetMapping("/{userId}/slack")
    public UserSlackInfo getUserSlackId(@PathVariable UUID userId) {
        return userService.getUserSlackId(userId);
    }
}
