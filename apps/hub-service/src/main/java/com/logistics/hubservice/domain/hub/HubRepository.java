package com.logistics.hubservice.domain.hub;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubRepository {

    Hub save(Hub hub);

    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);

    Page<Hub> search(String keyword, Pageable pageable);
}
