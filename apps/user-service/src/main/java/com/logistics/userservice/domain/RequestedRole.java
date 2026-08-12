package com.logistics.userservice.domain;

public enum RequestedRole {
    MASTER,
    HUB_MANAGER,
    COMPANY_MANAGER,
    HUB_DELIVERY_MANAGER,
    COMPANY_DELIVERY_MANAGER;

    public Role toRole() {
        return switch (this) {
            case MASTER -> Role.MASTER;
            case HUB_MANAGER -> Role.HUB_MANAGER;
            case COMPANY_MANAGER -> Role.COMPANY_MANAGER;
            case HUB_DELIVERY_MANAGER, COMPANY_DELIVERY_MANAGER -> Role.DELIVERY_MANAGER;
        };
    }
}
