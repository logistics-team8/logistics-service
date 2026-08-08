package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
}