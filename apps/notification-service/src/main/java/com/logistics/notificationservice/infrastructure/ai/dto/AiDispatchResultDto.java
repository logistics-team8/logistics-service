package com.logistics.notificationservice.infrastructure.ai.dto;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public record AiDispatchResultDto(
        String finalDispatchDeadline
) {

    private static final DateTimeFormatter FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:")
                    .appendValue(
                            ChronoField.SECOND_OF_MINUTE
                    )
                    .toFormatter();

    public LocalDateTime toFinalDispatchDeadline() {
        return LocalDateTime.parse(
                finalDispatchDeadline,
                FORMATTER
        );
    }
}