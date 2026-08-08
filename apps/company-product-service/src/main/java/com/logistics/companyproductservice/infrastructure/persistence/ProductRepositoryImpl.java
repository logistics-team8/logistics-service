package com.logistics.companyproductservice.infrastructure.persistence;

import com.logistics.companyproductservice.domain.model.Product;
import com.logistics.companyproductservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
    public Optional<Product> findById(UUID id) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Product> search(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return productJpaRepository.findAllByNameContainingAndDeletedAtIsNull(name, pageable);
        }
        return productJpaRepository.findAllByDeletedAtIsNull(pageable);
    }
}