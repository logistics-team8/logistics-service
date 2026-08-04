package com.logistics.orderservice.application.service;

import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;


    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand command) {
        Order order = Order.create(
                generateOrderNumber(),
                command.requesterId(),
                command.receiverCompanyId(),
                command.requestMessage(),
                command.requestedDeliveryAt()
        );

        for (CreateOrderItemCommand item : command.items()) {
            order.addOrderItem(
                    item.productId(),
                    item.quantity()
            );
        }

        return CreateOrderResponse.from(orderRepository.save(order));
    }


    private String generateOrderNumber() {
        String date = LocalDate.now()
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        int randomNumber = ThreadLocalRandom.current()
                .nextInt(100_000, 1_000_000);

        return "ORD-" + date + "-" + randomNumber;
    }
}
