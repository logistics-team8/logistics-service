package com.logistics.userservice.presentation.dto.admin;

import com.logistics.userservice.application.dto.admin.SearchUsersQuery;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.UserStatus;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminSearchRequest(
        @Size(max = 10, message = "아이디는 10자 이하로 입력해야 합니다.") String username,
        @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.") String name,
        UUID hubId,
        UUID companyId,
        Role role) {
    public SearchUsersQuery toQuery() {
        return new SearchUsersQuery(username, name, hubId, companyId, role, UserStatus.PENDING);
    }
}
