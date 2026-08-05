package com.logistics.hubservice.application.hub.query;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HubQueryService {

    private final HubRepository hubRepository;

    public HubResponse getOne(UUID hubId) {
        return HubResponse.from(findActiveHub(hubId));
    }

    public List<HubResponse> getAll() {
        return hubRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(HubResponse::from)
                .toList();
    }

    private Hub findActiveHub(UUID hubId) {
        return hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }
}
