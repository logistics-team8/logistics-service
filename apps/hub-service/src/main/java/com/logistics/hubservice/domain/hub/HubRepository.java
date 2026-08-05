package com.logistics.hubservice.domain.hub;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository {

    Hub save(Hub hub);

    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);

    List<Hub> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
