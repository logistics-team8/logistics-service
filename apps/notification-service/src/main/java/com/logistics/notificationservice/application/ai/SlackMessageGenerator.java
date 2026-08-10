package com.logistics.notificationservice.application.ai;

import com.logistics.notificationservice.infrastructure.ai.dto.AiDispatchResultDto;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class SlackMessageGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");

     public String generate (
             AiDispatchCommand command,
             AiDispatchResultDto result
     ){

         String products = command.products().stream()
                 .map(product -> "%s %d개"
                         .formatted(product.productName(),product.quantity()))
                         .collect(Collectors.joining(","));

         String transitHubs = String.join(",",command.transitHubs());

         return """
                주문 번호: %s
                주문자 정보: %s
                주문 시간: %s
                상품 정보: %s
                요청 사항: %s
                발송지: %s
                경유지: %s
                도착지: %s
                배송 담당자: %s

                위 내용을 기준으로 도출된 최종 발송 시한은 %s입니다.

                """
                 .formatted(
                         command.orderNumber(),
                         command.requesterName(),
                         command.requestedDeliveryAt(),
                         products,
                         command.requestMessage(),
                         command.departureHub(),
                         transitHubs,
                         command.destination(),
                         command.deliveryManagerName(),
                         result.toFinalDispatchDeadline()
                                 .format(FORMATTER)
                 );
     }
}
