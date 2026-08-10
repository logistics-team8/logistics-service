package com.logistics.hubservice.application.hub.query;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.PageableUtil;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.application.hub.dto.HubResponse;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HubQueryService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt", "updatedAt");

    private final HubRepository hubRepository;

    @Cacheable(cacheNames = "hubById", key = "#hubId")
    public HubResponse getOne(UUID hubId) {
        return HubResponse.from(findActiveHub(hubId));
    }

    public Page<HubResponse> search(String keyword, Pageable pageable) {
        Pageable normalizedPageable = PageableUtil.normalize(pageable, ALLOWED_SORT_PROPERTIES);
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<Hub> hubs = normalizedKeyword == null
                ? hubRepository.findAllByDeletedAtIsNull(normalizedPageable)
                : hubRepository.search(normalizedKeyword, normalizedPageable);

        return hubs
                .map(HubResponse::from);
    }

    private Hub findActiveHub(UUID hubId) {
        return hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalizedKeyword = keyword.strip().toLowerCase(Locale.ROOT);
        return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
    }
}
