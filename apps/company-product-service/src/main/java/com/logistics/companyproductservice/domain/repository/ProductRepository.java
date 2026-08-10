package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Product> findByIdForUpdate(UUID id);
    List<Product> findAllByIds(List<UUID> ids);
    Page<Product> search(String name, UUID hubId, Pageable pageable);
}