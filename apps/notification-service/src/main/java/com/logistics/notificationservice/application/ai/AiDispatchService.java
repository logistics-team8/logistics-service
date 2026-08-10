package com.logistics.notificationservice.application.ai;

import com.logistics.notificationservice.domain.ai.AiRequestLog;
import com.logistics.notificationservice.domain.ai.AiRequestLogRepository;
import com.logistics.notificationservice.infrastructure.ai.dto.AiDispatchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiDispatchService {


    private final DispatchPromptGenerator promptGenerator;
    private final GeminiClient geminiClient;
    private final AiRequestLogRepository aiRequestLogRepository;

    @Transactional
    public AiDispatchProcessResult calculateDeadline(
            AiDispatchCommand command
    ) {

        String prompt = promptGenerator.generate(command);

        AiRequestLog log =
                AiRequestLog.create(
                        command.orderId(),
                        prompt
                );

        aiRequestLogRepository.save(log);

        try {

            GeminiClient.GeminiResult geminiResult =
                    geminiClient.generateDispatchDeadline(prompt);

            log.success(
                    geminiResult.rawResponse(),
                    geminiResult.result().toFinalDispatchDeadline(),
                    geminiResult.modelName(),
                    geminiResult.responseTimeMs()
            );

            return new AiDispatchProcessResult(
                    log.getAiRequestId(),
                    geminiResult.result()
            );

        } catch (Exception e) {

            log.fail(e.getMessage());
            throw e;
        }
    }
}