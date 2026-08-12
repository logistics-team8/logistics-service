package com.logistics.userservice.application.port;

import com.logistics.userservice.application.dto.admin.SearchUsersQuery;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserQueryRepository {
    Page<User> searchUsers(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable);

    Page<User> searchPendingUsers(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable);
}
