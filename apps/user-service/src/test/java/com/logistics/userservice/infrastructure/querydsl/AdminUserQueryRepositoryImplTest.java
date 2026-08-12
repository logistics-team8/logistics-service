package com.logistics.userservice.infrastructure.querydsl;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.logistics.userservice.application.dto.admin.SearchUsersQuery;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.application.dto.user.UserCreateCommand;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.config.test.AbstractIntegrationTest;
import com.logistics.userservice.domain.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
class AdminUserQueryRepositoryTest extends AbstractIntegrationTest {
    @Autowired private AdminUserQueryRepository adminUserQueryRepository;
    @Autowired private UserRepository userRepository;

    private UUID dummyUserHubId;
    private UUID dummyUserHubId2;

    private UUID dummyUserCompany;

    @BeforeEach
    void setUp() {
        dummyUserHubId = UUID.randomUUID();
        dummyUserHubId2 = UUID.randomUUID();
        dummyUserCompany = UUID.randomUUID();

        createUser("test1234", "U111111111", dummyUserHubId, null, RequestedRole.HUB_MANAGER)
                .approve(UUID.randomUUID());

        createUser(
                        "test2345",
                        "U222222222",
                        dummyUserHubId,
                        dummyUserCompany,
                        RequestedRole.COMPANY_MANAGER)
                .approve(UUID.randomUUID());

        createUser(
                        "test3456",
                        "U333333333",
                        dummyUserHubId2,
                        null,
                        RequestedRole.HUB_DELIVERY_MANAGER)
                .approve(UUID.randomUUID());
        userRepository.flush();
    }

    @Test
    @DisplayName("회원 이름으로 검색이 가능하다.")
    void searchUsers_success_search_by_username() {
        // given
        UserContext userContext = new UserContext(UUID.randomUUID(), Role.MASTER, null);

        SearchUsersQuery searchUsersQuery =
                new SearchUsersQuery("test123", null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        // when
        Page<User> result =
                adminUserQueryRepository.searchUsers(userContext, searchUsersQuery, pageable);

        // then
        assertThat(result.getContent()).extracting(User::getUsername).containsExactly("test1234");
    }

    @Test
    @DisplayName("허브 관리자는 자신이 속한 허브만 검색이 가능하다.")
    void searchUsers_success_when_search_by_hub_manager() {
        // given
        UserContext userContext =
                new UserContext(UUID.randomUUID(), Role.HUB_MANAGER, dummyUserHubId2);

        SearchUsersQuery searchUsersQuery =
                new SearchUsersQuery(null, null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        // when
        Page<User> result =
                adminUserQueryRepository.searchUsers(userContext, searchUsersQuery, pageable);

        // then
        assertThat(result.getContent()).extracting(User::getUsername).containsExactly("test3456");
    }

    @Test
    @DisplayName("Master는 특정 허브만 검색이 가능하다.")
    void searchUsers_success_search_by_hub() {
        // given
        UserContext userContext = new UserContext(UUID.randomUUID(), Role.MASTER, null);

        SearchUsersQuery searchUsersQuery =
                new SearchUsersQuery(null, null, dummyUserHubId2, null, null, null);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        // when
        Page<User> result =
                adminUserQueryRepository.searchUsers(userContext, searchUsersQuery, pageable);

        // then
        assertThat(result.getContent()).extracting(User::getUsername).containsExactly("test3456");
    }

    @Test
    @DisplayName("특정 업체 유저만 검색이 가능하다.")
    void searchUsers_success_search_by_company() {
        // given
        UserContext userContext = new UserContext(UUID.randomUUID(), Role.MASTER, null);

        SearchUsersQuery searchUsersQuery =
                new SearchUsersQuery(null, null, null, dummyUserCompany, null, null);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        // when
        Page<User> result =
                adminUserQueryRepository.searchUsers(userContext, searchUsersQuery, pageable);

        // then
        assertThat(result.getContent()).extracting(User::getUsername).containsExactly("test2345");
    }

    @Test
    @DisplayName("특정 권한 유저만 검색이 가능하다.")
    void searchUsers_success_search_by_role() {
        // given
        UserContext userContext = new UserContext(UUID.randomUUID(), Role.MASTER, null);

        SearchUsersQuery searchUsersQuery =
                new SearchUsersQuery(null, null, null, null, Role.COMPANY_MANAGER, null);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        // when
        Page<User> result =
                adminUserQueryRepository.searchUsers(userContext, searchUsersQuery, pageable);

        // then
        assertThat(result.getContent()).extracting(User::getUsername).containsExactly("test2345");
    }

    @Test
    @DisplayName("특정 상태 유저만 검색이 가능하다.")
    void searchUsers_success_search_by_userStatus() {
        // given
        UserContext userContext = new UserContext(UUID.randomUUID(), Role.MASTER, null);

        SearchUsersQuery searchUsersQuery =
                new SearchUsersQuery(null, null, null, null, null, UserStatus.PROCESSING);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

        // when
        Page<User> result =
                adminUserQueryRepository.searchUsers(userContext, searchUsersQuery, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private User createUser(
            String username,
            String slackId,
            UUID hubId,
            UUID companyId,
            RequestedRole requestedRole) {

        UserCreateCommand command =
                new UserCreateCommand(
                        username,
                        "Testtest123!",
                        UUID.randomUUID().toString(),
                        slackId,
                        hubId,
                        companyId,
                        requestedRole);

        return userRepository.save(User.create(command));
    }
}
