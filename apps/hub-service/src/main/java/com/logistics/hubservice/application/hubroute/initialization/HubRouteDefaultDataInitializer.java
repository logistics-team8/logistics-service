package com.logistics.hubservice.application.hubroute.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class HubRouteDefaultDataInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(HubRouteDefaultDataInitializer.class);

    private final HubRouteDefaultDataService service;

    public HubRouteDefaultDataInitializer(HubRouteDefaultDataService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        HubRouteDefaultDataResult result = service.initialize();
        log.info(
                "기본 HubRoute 데이터 초기화 완료: Hub {}개, HubRoute {}개 생성",
                result.createdHubCount(),
                result.createdHubRouteCount());
    }
}
