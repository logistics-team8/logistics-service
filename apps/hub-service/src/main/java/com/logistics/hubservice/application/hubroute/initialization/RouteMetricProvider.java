package com.logistics.hubservice.application.hubroute.initialization;

public interface RouteMetricProvider {

    RouteMetric getMetric(HubCoordinates source, HubCoordinates destination);
}
