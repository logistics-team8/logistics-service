package com.logistics.notificationservice.infrastructure.persistence;

import com.logistics.notificationservice.domain.slack.SlackMessage;
import com.logistics.notificationservice.application.slack.SlackMessageSearchCondition;
import org.springframework.data.jpa.domain.Specification;

public final class SlackMessageSpecification {

    private SlackMessageSpecification() {
    }


    public static Specification<SlackMessage> search(
            SlackMessageSearchCondition condition
    ) {

        Specification<SlackMessage> spec =
                (root, query, cb) -> cb.conjunction();

        if (condition.status() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("status"),
                                    condition.status()
                            )
            );
        }

        if (condition.orderId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("orderId"),
                                    condition.orderId()
                            )
            );
        }

        if (condition.recipientUserId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("recipientUserId"),
                                    condition.recipientUserId()
                            )
            );
        }

        if (condition.recipientSlackId() != null
                && !condition.recipientSlackId().isBlank()) {

            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("recipientSlackId"),
                                    condition.recipientSlackId()
                            )
            );
        }

        if (condition.keyword() != null
                && !condition.keyword().isBlank()) {

            String keyword =
                    "%" + condition.keyword().toLowerCase() + "%";

            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("message")),
                                    keyword
                            )
            );
        }

        return spec;
    }

    private static Specification<SlackMessage> statusEq(
            SlackMessageSearchCondition condition
    ) {

        if (condition.status() == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(
                        root.get("status"),
                        condition.status()
                );
    }

    private static Specification<SlackMessage> orderIdEq(
            SlackMessageSearchCondition condition
    ) {

        if (condition.orderId() == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("orderId"), condition.orderId()
                );
    }

    private static Specification<SlackMessage> recipientUserIdEq(
            SlackMessageSearchCondition condition
    ) {

        if (condition.recipientUserId() == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("recipientUserId"), condition.recipientUserId()
                );
    }

    private static Specification<SlackMessage> recipientSlackIdEq(
            SlackMessageSearchCondition condition
    ) {

        if (condition.recipientSlackId() == null || condition.recipientSlackId().isBlank()) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("recipientSlackId"), condition.recipientSlackId());
    }

    private static Specification<SlackMessage> messageContains(
            SlackMessageSearchCondition condition
    ) {

        if (condition.keyword() == null || condition.keyword().isBlank()) {
            return null;
        }

        return (root, query, cb) ->
                cb.like(cb.lower(root.get("message")), "%" + condition.keyword().toLowerCase() + "%");
    }
}

