package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class IdempotentOrderCreateService {
    private static final int MAX_KEY_LENGTH = 100;
    private final OrderRepository orderRepository;
    private final OrderCreateService orderCreateService;

    public CreateOrderResponse createOrder(
            String idempotencyKey,
            CreateOrderCommand command,
            CustomUserDetails user)
    {
        String normalizedKey = validateAndNormalize(idempotencyKey);
        String requestHash = createRequestHash(command);

        return orderRepository
                .findByRequesterIdAndIdempotencyKey(user.getId(), normalizedKey)
                .map(order -> replayOrReject(order, requestHash))
                .orElseGet(() -> createOrReplay(
                        command,
                        user,
                        normalizedKey,
                        requestHash
                ));

    }



    private CreateOrderResponse  createOrReplay(CreateOrderCommand command, CustomUserDetails user, String idempotencyKey, String requestHash) {
        try {
            return orderCreateService.createOrder(command,user,idempotencyKey,requestHash);
        }catch (DataIntegrityViolationException exception){
            Order existingOrder  = orderRepository
                    .findByRequesterIdAndIdempotencyKey(user.getId(), idempotencyKey)
                    .orElseThrow(() -> exception);

            return replayOrReject(existingOrder, requestHash);
        }

    }

    private CreateOrderResponse replayOrReject(Order existingOrder, String currentRequestHash) {
        if(!Objects.equals(existingOrder.getRequestHash(), currentRequestHash)) {
            throw new BusinessException(OrderErrorCode.IDEMPOTENCY_REQUEST_CONFLICT);
        }
        return CreateOrderResponse.existing(existingOrder);
    }


    private String validateAndNormalize(String idempotencyKey) {
        if (idempotencyKey == null) {
            throw new BusinessException(
                    OrderErrorCode.IDEMPOTENCY_KEY_INVALID
            );
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.isEmpty()
                || normalizedKey.length() > MAX_KEY_LENGTH) {
            throw new BusinessException(
                    OrderErrorCode.IDEMPOTENCY_KEY_INVALID
            );
        }

        return normalizedKey;
    }

    private String createRequestHash(
            CreateOrderCommand command
    ) {
        String canonicalItems = command.items()
                .stream()
                .sorted(Comparator.comparing(
                        CreateOrderItemCommand::productId
                ))
                .map(item ->
                        item.productId()
                                + ":"
                                + item.quantity()
                )
                .collect(Collectors.joining(","));

        String canonicalRequest =
                command.receiverCompanyId()
                        + "|"
                        + Objects.toString(
                        command.requestMessage(),
                        ""
                )
                        + "|"
                        + command.requestedDeliveryAt()
                        + "|"
                        + canonicalItems;

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(
                    canonicalRequest.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}
