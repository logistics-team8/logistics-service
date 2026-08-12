package com.logistics.userservice.infrastructure.querydsl;

import com.logistics.userservice.application.dto.admin.SearchUsersQuery;
import com.logistics.userservice.application.dto.user.UserContext;
import com.logistics.userservice.application.port.AdminUserQueryRepository;
import com.logistics.userservice.domain.QUser;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminUserQueryRepositoryImpl implements AdminUserQueryRepository {
    private static final QUser user = QUser.user;
    private final JPAQueryFactory queryFactory;

    /**
     * 관리자 회원 조회
     *
     * @param userContext
     * @param searchUsersQuery
     * @param pageable
     * @return
     */
    @Override
    public Page<User> searchUsers(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable) {
        BooleanBuilder builder =
                commonBuilder(userContext, searchUsersQuery)
                        .and(QuerydslUtils.eq(user.userStatus, searchUsersQuery.userStatus()));

        return commonQuery(builder, pageable);
    }

    /**
     * 승인 유저 조회
     *
     * @param userContext
     * @param searchUsersQuery
     * @param pageable
     * @return
     */
    @Override
    public Page<User> searchPendingUsers(
            UserContext userContext, SearchUsersQuery searchUsersQuery, Pageable pageable) {
        // PENDING 상태 고정
        BooleanBuilder builder =
                commonBuilder(userContext, searchUsersQuery)
                        .and(user.userStatus.eq(UserStatus.PENDING));

        return commonQuery(builder, pageable);
    }

    /**
     * 공통 쿼리 조건 빌더
     *
     * @param userContext 검색자 정보
     * @param searchUsersQuery 쿼리 조건
     * @return BooleanBuilder
     */
    private BooleanBuilder commonBuilder(
            UserContext userContext, SearchUsersQuery searchUsersQuery) {
        BooleanBuilder builder = new BooleanBuilder();

        // 허브 매니저인 경우 본인 허브 요청만 조회 가능
        if (userContext.isHubManager()) {
            builder.and(QuerydslUtils.eq(user.hubId, userContext.hubId()));
        } else {
            builder.and(QuerydslUtils.eq(user.hubId, searchUsersQuery.hubId()));
        }

        // 공통 쿼리
        return builder.and(QuerydslUtils.startsWith(user.username, searchUsersQuery.username()))
                .and(QuerydslUtils.startsWith(user.name, searchUsersQuery.name()))
                .and(QuerydslUtils.eq(user.companyId, searchUsersQuery.companyId()))
                .and(QuerydslUtils.eq(user.role, searchUsersQuery.role()));
    }

    /**
     * DB 조회 및 페이징 처리
     *
     * @param builder
     * @param pageable
     * @return
     */
    private Page<User> commonQuery(BooleanBuilder builder, Pageable pageable) {
        OrderSpecifier<?>[] order = QuerydslUtils.getSort(pageable, user);

        List<User> result =
                queryFactory
                        .selectFrom(user)
                        .where(builder)
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .orderBy(order)
                        .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(user.count()).from(user).where(builder);

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }
}
