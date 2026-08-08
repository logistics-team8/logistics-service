package com.logistics.hubservice.infrastructure.persistence.hub;

import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class HubJpaRepositoryAdapter implements HubRepository {

    private final SpringDataHubRepository repository;

    public HubJpaRepositoryAdapter(SpringDataHubRepository repository) {
        this.repository = repository;
    }

    @Override
    public Hub save(Hub hub) {
        return repository.save(hub);
    }

    @Override
    public Optional<Hub> findByIdAndDeletedAtIsNull(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Hub> findAllByDeletedAtIsNull(Pageable pageable) {
        return repository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public Page<Hub> search(String keyword, Pageable pageable) {
        return repository.search(keyword, pageable);
    }
}
