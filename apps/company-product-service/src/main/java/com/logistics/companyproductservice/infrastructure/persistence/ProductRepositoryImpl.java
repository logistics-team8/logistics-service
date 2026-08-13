package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findByIdAndDeletedAtIsNull(UUID id) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Product> findByIdForUpdate(UUID id) {
        return productJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Page<Product> search(String name, UUID hubId, Pageable pageable) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasHub = hubId != null;

        if (hasName && hasHub) {
            return productJpaRepository.findAllByNameContainingAndHubIdAndDeletedAtIsNull(name, hubId, pageable);
        }
        if (hasHub) {
            return productJpaRepository.findAllByHubIdAndDeletedAtIsNull(hubId, pageable);
        }
        if (hasName) {
            return productJpaRepository.findAllByNameContainingAndDeletedAtIsNull(name, pageable);
        }
        return productJpaRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Override
    public List<Product> findAllByIds(List<UUID> ids) {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }
}