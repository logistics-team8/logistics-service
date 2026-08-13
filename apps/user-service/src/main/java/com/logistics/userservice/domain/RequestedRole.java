package com.logistics.userservice.domain;

public enum RequestedRole {
    MASTER,
    HUB_MANAGER,
    COMPANY_MANAGER,
    HUB_DELIVERY,
    COMPANY_DELIVERY;

    public Role toRole() {
        return switch (this) {
            case MASTER -> Role.MASTER;
            case HUB_MANAGER -> Role.HUB_MANAGER;
            case COMPANY_MANAGER -> Role.COMPANY_MANAGER;
            case HUB_DELIVERY, COMPANY_DELIVERY -> Role.DELIVERY_MANAGER;
        };
    }
}
