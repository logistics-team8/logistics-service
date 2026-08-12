package com.logistics.hubservice.application.hubroute.initialization;

import java.util.List;
import java.util.stream.Stream;

public final class DefaultHubRouteTopology {

    private static final List<DefaultHubConnection> CONNECTIONS = List.of(
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.GYEONGGI_NORTH),
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.SEOUL),
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.INCHEON),
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.GANGWON),
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.GYEONGSANGBUK),
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.DAEJEON),
            connection(DefaultHub.GYEONGGI_SOUTH, DefaultHub.DAEGU),
            connection(DefaultHub.DAEJEON, DefaultHub.CHUNGCHEONGNAM),
            connection(DefaultHub.DAEJEON, DefaultHub.CHUNGCHEONGBUK),
            connection(DefaultHub.DAEJEON, DefaultHub.SEJONG),
            connection(DefaultHub.DAEJEON, DefaultHub.JEONBUK),
            connection(DefaultHub.DAEJEON, DefaultHub.GWANGJU),
            connection(DefaultHub.DAEJEON, DefaultHub.JEOLLANAM),
            connection(DefaultHub.DAEJEON, DefaultHub.DAEGU),
            connection(DefaultHub.DAEGU, DefaultHub.GYEONGSANGBUK),
            connection(DefaultHub.DAEGU, DefaultHub.GYEONGSANGNAM),
            connection(DefaultHub.DAEGU, DefaultHub.BUSAN),
            connection(DefaultHub.DAEGU, DefaultHub.ULSAN));

    private static final List<DefaultHubConnection> DIRECTED_CONNECTIONS = CONNECTIONS.stream()
            .flatMap(connection -> Stream.of(connection, connection.reverse()))
            .toList();

    private DefaultHubRouteTopology() {
    }

    public static List<DefaultHubConnection> connections() {
        return CONNECTIONS;
    }

    public static List<DefaultHubConnection> directedConnections() {
        return DIRECTED_CONNECTIONS;
    }

    private static DefaultHubConnection connection(DefaultHub source, DefaultHub destination) {
        return new DefaultHubConnection(source, destination);
    }
}
