package com.logistics.orderservice.application.authorization;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.error.OrderErrorCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;


import java.util.UUID;

@Component
public class OrderAuthorization {

    private static final String ROLE_MASTER = "ROLE_MASTER";
    private static final String ROLE_HUB_MANAGER = "ROLE_HUB_MANAGER";

    /**
     * 주문 수정/삭제 권한 검사
     */
    public void validateOrderManagementPermission(CustomUserDetails user, Order order) {
        if(hasRole(user, ROLE_MASTER)){
            return;
        }
        if (hasRole(user, ROLE_HUB_MANAGER) && isManagedHubOrder(user, order)) {
            return;
        }
        throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
    }


    /**
     * 주문 취소 권한 검사
     */
    public void validateCancelPermission(CustomUserDetails user, Order order) {
        if(hasRole(user, ROLE_MASTER)){
            return;
        }
        if (hasRole(user, ROLE_HUB_MANAGER) && isManagedHubOrder(user, order)) {
            return;
        }
        //본인 주문에 대한 취소 가능
        if (user.getId().equals(order.getRequesterId())) {
            return;
        }
        throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
    }

    /**
     *  로그인한 허브 매니저가 해당 주문을 담당하는 허브관리자인지 확인한다.
     */
    private boolean isManagedHubOrder(CustomUserDetails user, Order order) {
        UUID userHubId = user.getHubId();

        boolean destinationHubOrder =
                order.getDestinationHubId() != null && userHubId.equals(order.getDestinationHubId());
        if(destinationHubOrder){
            return true;
        }

        return order.getOrderItems().stream()
                .anyMatch(orderItem ->
                        orderItem.getDepartureHubId() != null && userHubId.equals(orderItem.getDepartureHubId()
                        )
                );
    }

    private boolean hasRole(CustomUserDetails user, String role) {
        return user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
