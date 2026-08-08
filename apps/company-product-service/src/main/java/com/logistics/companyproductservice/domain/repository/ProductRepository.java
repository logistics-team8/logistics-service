package com.logistics.companyproductservice.domain.repository;

import com.logistics.companyproductservice.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Page<Product> search(String name, Pageable pageable);
}