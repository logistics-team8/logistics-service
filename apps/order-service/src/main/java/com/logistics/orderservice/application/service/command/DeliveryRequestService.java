package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.DeliveryCreateRejectedException;
import com.logistics.orderservice.application.exception.DeliveryCreateRetryableException;
import com.logistics.orderservice.application.exception.DeliveryLookupException;
import com.logistics.orderservice.application.exception.DeliveryStatusUnknownException;
import com.logistics.orderservice.application.port.DeliveryPort;
import com.logistics.orderservice.error.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryRequestService {
    private final DeliveryPort deliveryPort;

    public Optional<DeliveryPort.DeliveryInfo> requestDelivery(DeliveryPort.CreateDeliveryCommand command) {

        //최초 배송 생성 요청을 시도
        try{
            DeliveryPort.DeliveryInfo deliveryInfo = deliveryPort.createDelivery(command);
            //정상 응답 반환 시 배송 생성 성공
            return Optional.of(deliveryInfo);
        } catch (DeliveryCreateRejectedException e) {
            log.warn("재시도 불가능한 배송 생성 실패. orderId : {}", command.orderId(), e);
            return Optional.empty();
        } catch (DeliveryCreateRetryableException e) {
            log.warn("일시적 배송 생성 실패. 배송 생성 여부 확인. orderId : {}", command.orderId(), e);
        }

        //배송 생성 실패 후 배송 조회
        Optional<DeliveryPort.DeliveryInfo> existingDelivery  = findDelivery(command);

        //배송이 존재하면 재시도 하지 않고 기존 배송 정보를 반환
        if(existingDelivery.isPresent()) {
            return existingDelivery;
        }


        /**
         * 배송 조회 요청은 성공했고 실제로 해당 주문의 배송이 존재하지 않는 상태
         * Delivery의 orderId 멱등성을 이용해
         * 같은 payload로 한 번 재요청
         */
        try {
            DeliveryPort.DeliveryInfo delivery = deliveryPort.createDelivery(command);
            return Optional.of(delivery);
        } catch (DeliveryCreateRejectedException e) {
            log.warn("배송 생성 재요청이 명확하게 거절됨. orderId : {}", command.orderId(), e);
            return Optional.empty();
        } catch (DeliveryCreateRetryableException e) {
            log.warn("배송 생성 재요청 결과 확인 불가. 최종 상태 조회. orderId : {}", command.orderId(), e);
        }

        //재시도 실패
        //다시 delivery가 있는 지 확인한다
        return findDelivery(command);
    }


    private Optional<DeliveryPort.DeliveryInfo> findDelivery(DeliveryPort.CreateDeliveryCommand command) {
        Optional<DeliveryPort.DeliveryInfo> delivery;

        try{
            delivery = deliveryPort.findDeliveryByOrderId(command.orderId());
        }catch (DeliveryLookupException e) {
            //조회 자체가 실패하면 배송이 없는 것이 아니라 배송 존재 여부를 모르는 상태로
            //재고를 복원하면 안된다.
            log.error("배송 생성 결과 확인 실패 : orderId : {}", command.orderId(), e);
            throw new DeliveryStatusUnknownException("배송 생성 결과를 확인할 수 없습니다.",e);

        }
            //조회는 성공했지만 배송이 존재하지 않는다.
           if(delivery.isEmpty()){
               return Optional.empty();
           }

           //배송 정보 가져옴
           DeliveryPort.DeliveryInfo existing = delivery.get();

           //기존 배송과 현재 요청 payload와 일치하는지도 검증한다.
            if(!existing.matches(command)) {
                log.error("기존 배송 정보가 주문 배송 요청과 일치하지 않습니다. orderId : {}, deliveryId : {}", command.orderId(), existing.deliveryId());
                throw new BusinessException(OrderErrorCode.DELIVERY_REQUEST_CONFLICT);
            }

        log.info("배송 생성 요청은 실패했으나 기존 배송 확인 성공. orderId : {}, deliveryId : {}", command.orderId(), existing.deliveryId());

        return Optional.of(existing);
    }
}
