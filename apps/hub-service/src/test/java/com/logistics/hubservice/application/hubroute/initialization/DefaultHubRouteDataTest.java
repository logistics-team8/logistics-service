package com.logistics.hubservice.application.hubroute.initialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultHubRouteDataTest {

    @Test
    @DisplayName("기본 허브 데이터는 확정된 17개 센터를 고정 ID로 제공한다")
    void providesSeventeenDefaultHubsWithStableIds() {
        assertThat(DefaultHub.values())
                .extracting(DefaultHub::hubName, DefaultHub::address)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "서울특별시 센터", "서울특별시 송파구 송파대로 55"),
                        org.assertj.core.groups.Tuple.tuple(
                                "경기 북부 센터", "경기도 고양시 덕양구 권율대로 570"),
                        org.assertj.core.groups.Tuple.tuple(
                                "경기 남부 센터", "경기도 이천시 덕평로 257-21"),
                        org.assertj.core.groups.Tuple.tuple(
                                "부산광역시 센터", "부산 동구 중앙대로 206"),
                        org.assertj.core.groups.Tuple.tuple(
                                "대구광역시 센터", "대구 북구 태평로 161"),
                        org.assertj.core.groups.Tuple.tuple(
                                "인천광역시 센터", "인천 남동구 정각로 29"),
                        org.assertj.core.groups.Tuple.tuple(
                                "광주광역시 센터", "광주 서구 내방로 111"),
                        org.assertj.core.groups.Tuple.tuple(
                                "대전광역시 센터", "대전 서구 둔산로 100"),
                        org.assertj.core.groups.Tuple.tuple(
                                "울산광역시 센터", "울산 남구 중앙로 201"),
                        org.assertj.core.groups.Tuple.tuple(
                                "세종특별자치시 센터", "세종특별자치시 한누리대로 2130"),
                        org.assertj.core.groups.Tuple.tuple(
                                "강원특별자치도 센터", "강원특별자치도 춘천시 중앙로 1"),
                        org.assertj.core.groups.Tuple.tuple(
                                "충청북도 센터", "충북 청주시 상당구 상당로 82"),
                        org.assertj.core.groups.Tuple.tuple(
                                "충청남도 센터", "충남 홍성군 홍북읍 충남대로 21"),
                        org.assertj.core.groups.Tuple.tuple(
                                "전북특별자치도 센터", "전북특별자치도 전주시 완산구 효자로 225"),
                        org.assertj.core.groups.Tuple.tuple(
                                "전라남도 센터", "전남 무안군 삼향읍 오룡길 1"),
                        org.assertj.core.groups.Tuple.tuple(
                                "경상북도 센터", "경북 안동시 풍천면 도청대로 455"),
                        org.assertj.core.groups.Tuple.tuple(
                                "경상남도 센터", "경남 창원시 의창구 중앙대로 300"));

        Set<?> uniqueIds = java.util.Arrays.stream(DefaultHub.values())
                .map(DefaultHub::hubId)
                .collect(Collectors.toSet());

        assertThat(uniqueIds).hasSize(17);
    }

    @Test
    @DisplayName("기본 연결 구조는 중복 없이 18개 구간을 양방향으로 제공한다")
    void providesEighteenBidirectionalConnections() {
        assertThat(DefaultHubRouteTopology.connections()).containsExactly(
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
        assertThat(DefaultHubRouteTopology.directedConnections()).hasSize(36);
        assertThat(DefaultHubRouteTopology.directedConnections())
                .doesNotHaveDuplicates()
                .noneMatch(connection -> connection.source() == connection.destination());
    }

    private DefaultHubConnection connection(DefaultHub source, DefaultHub destination) {
        return new DefaultHubConnection(source, destination);
    }
}
