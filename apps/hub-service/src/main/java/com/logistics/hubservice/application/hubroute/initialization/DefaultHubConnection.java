package com.logistics.hubservice.application.hubroute.initialization;

public record DefaultHubConnection(DefaultHub source, DefaultHub destination) {

    public DefaultHubConnection reverse() {
        return new DefaultHubConnection(destination, source);
    }
}
