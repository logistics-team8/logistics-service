package com.logistics.userservice.infrastructure.querydsl;

import com.logistics.userservice.application.dto.admin.SearchUsersQuery;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.domain.QUser;
import com.logistics.userservice.domain.User;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminUserQueryRepositoryImpl implements AdminUserQueryRepository {
    private static final QUser user = QUser.user;
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<User> searchUsers(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable) {
        OrderSpecifier<?>[] order = QuerydslUtils.getSort(pageable, user);

        List<User> result =
                queryFactory
                        .selectFrom(user)
                        .where(
                                QuerydslUtils.eq(
                                        user.hubId,
                                        userContext.isHubManager()
                                                ? userContext.hubId()
                                                : searchUsersQuery.hubId()),
                                QuerydslUtils.eq(user.userStatus, searchUsersQuery.userStatus()),
                                QuerydslUtils.startsWith(
                                        user.username, searchUsersQuery.username()),
                                QuerydslUtils.eq(user.companyId, searchUsersQuery.companyId()),
                                QuerydslUtils.eq(user.role, searchUsersQuery.role()))
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .orderBy(order)
                        .fetch();

        if (result.size() < pageable.getPageSize() && pageable.getOffset() == 0) {
            return new PageImpl<>(result, pageable, result.size());
        }

        Long total =
                queryFactory
                        .select(user.count())
                        .from(user)
                        .where(
                                QuerydslUtils.eq(
                                        user.hubId,
                                        userContext.isHubManager()
                                                ? userContext.hubId()
                                                : searchUsersQuery.hubId()),
                                QuerydslUtils.eq(user.userStatus, searchUsersQuery.userStatus()),
                                QuerydslUtils.startsWith(
                                        user.username, searchUsersQuery.username()),
                                QuerydslUtils.eq(user.companyId, searchUsersQuery.companyId()),
                                QuerydslUtils.eq(user.role, searchUsersQuery.role()))
                        .fetchOne();

        return new PageImpl<>(result, pageable, total == null ? 0 : total);
    }
}
