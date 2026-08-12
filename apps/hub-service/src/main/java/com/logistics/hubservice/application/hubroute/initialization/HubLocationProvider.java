package com.logistics.hubservice.application.hubroute.initialization;

public interface HubLocationProvider {

    HubCoordinates geocode(String address);
}
