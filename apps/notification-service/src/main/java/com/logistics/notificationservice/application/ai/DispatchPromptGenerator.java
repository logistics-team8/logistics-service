package com.logistics.notificationservice.application.ai;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DispatchPromptGenerator {

    public String generate(AiDispatchCommand command) {

        String productInfo = command.products().stream()
                .map(product ->
                        "- 상품명: %s, 수량: %d"
                                .formatted(
                                        product.productName(),
                                        product.quantity()
                                )
                )
                .collect(Collectors.joining("\n"));

        String transitHubInfo = command.transitHubs().isEmpty()
                ? "없음"
                : String.join(" → ", command.transitHubs());

        return """
                당신은 물류 배송 일정 계산 전문가입니다.
                
                아래 주문 및 배송 정보를 모두 고려하여
                배송 담당자가 상품을 발송해야 하는 최종 발송 시한을 계산해주세요.
                
                [주문 정보]
                주문 ID: %s
                주문 번호: %s
                주문자: %s
                
                [상품 정보]
                %s
                
                [주문 요청사항]
                %s
                
                [요청 배송 일시]
                %s
                
                [배송 경로]
                발송지: %s
                경유지: %s
                도착지: %s
                
                [배송 담당자]
                이름: %s
                근무시간: %s ~ %s
                
                다음 규칙을 반드시 지켜주세요.
                
                1. 요청 배송 일시보다 늦게 도착하면 안 됩니다.
                2. 배송 담당자의 근무시간을 고려해야 합니다.
                3. 경유지가 여러 곳이면 모든 경유지를 고려해야 합니다.
                4. 정확한 교통 정보가 없으면 보수적으로 판단해주세요.
                5. 최종 발송 시한은 반드시 yyyy-MM-dd'T'HH:mm:ss 형식으로 작성해주세요.
                6. 연도는 4자리, 월/일/시/분/초는 반드시 2자리 숫자로 작성해주세요.
                7. 초가 0초인 경우에도 반드시 00으로 작성해주세요.
                8. 예: 2026-08-11T18:00:00
                9. 응답은 반드시 아래 JSON 형식으로만 작성해주세요.
                10. 다른 설명이나 문장은 추가하지 마세요.
                11. Markdown 코드 블록은 사용하지 마세요.
                
                {
                  "finalDispatchDeadline": "2026-08-11T18:00:00"
                }
                """
                .formatted(
                        command.orderId(),
                        command.orderNumber(),
                        command.requesterName(),
                        productInfo,
                        command.requestMessage(),
                        command.requestedDeliveryAt(),
                        command.departureHub(),
                        transitHubInfo,
                        command.destination(),
                        command.deliveryManagerName(),
                        command.workStartTime(),
                        command.workEndTime()
                );
    }
}